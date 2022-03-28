package util;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;

/**
 * Helper class for uploading files to a temporary hosting service.
 */
public class TemporaryFileUploader {
    private static final String UPLOAD_URL = "https://transfer.sh/";

    /**
     * Uploads a file to the temporary file hosting service.
     * @param file
     *  The file to upload
     * @return
     *  The URL returned by the server
     */
    public static String uploadAndGetURL(File file) throws IOException {
        try(CloseableHttpClient client = HttpClients.createDefault()) {
            HttpUriRequest request = RequestBuilder.put(UPLOAD_URL + file.getName())
                    .setEntity(new FileEntity(file))
                    .build();

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: %d".formatted(status));
                }
            };

            return client.execute(request, responseHandler);
        }
    }
}
