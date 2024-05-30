import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {
    private final int requestLimit;
    private final long timeWindow;
    private final Lock lock = new ReentrantLock();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastResetTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeWindow = timeUnit.toMillis(1);
        this.lastResetTime = System.currentTimeMillis();
    }

    public void createDocument(Document document, String signature) {
        try {
            lock.lock();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResetTime > timeWindow) {
                requestCount.set(0);
                lastResetTime = currentTime;
            }

            if (requestCount.get() >= requestLimit) {
                return;
            }

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
                httpPost.setHeader("Content-Type", "application/json");

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonDocument = objectMapper.writeValueAsString(document);

                StringEntity entity = new StringEntity(jsonDocument);
                httpPost.setEntity(entity);

                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity responseEntity = response.getEntity();
                // Handle response if needed

                requestCount.incrementAndGet();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        Document sampleDocument = new Document("sampleParticipantInn", "sampleDocId", "LP_INTRODUCE_GOODS");
        crptApi.createDocument(sampleDocument, "sampleSignature");
    }

    static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;

        public Document(String participantInn, String docId, String docType) {
            this.description = new Description(participantInn);
            this.doc_id = docId;
            this.doc_status = "pending";
            this.doc_type = docType;
            this.importRequest = true;
        }
    }

    static class Description {
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }
}
