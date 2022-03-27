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

public class FileUploader {
    private static final String UPLOAD_URL = "https://transfer.sh/";

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
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return client.execute(request, responseHandler);
        }
    }
}
