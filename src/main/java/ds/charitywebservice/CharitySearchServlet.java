package ds.charitywebservice;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/charities")
public class CharitySearchServlet extends HttpServlet {
    private static final String API_KEY = "0d7296069e66707b13b517638392731e";
    private static final String API_BASE_URL = "https://data.orghunter.com/v1/charitysearch";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Set response content type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get parameters
        String category = request.getParameter("category");
        String city = request.getParameter("city");

        // Validate parameters
        if (category == null || category.isEmpty() || city == null || city.isEmpty()) {
            sendErrorResponse(response, "Missing required parameters: category and city");
            return;
        }

        try {
            // Fetch data from third-party API
            JSONArray charities = fetchCharitiesFromAPI(category, city);

            // Process data and create response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("success", true);

            // Create a simplified array of charities with only needed information
            JSONArray simplifiedCharities = new JSONArray();
            for (int i = 0; i < charities.length(); i++) {
                JSONObject charity = charities.getJSONObject(i);
                JSONObject simplifiedCharity = new JSONObject();
                simplifiedCharity.put("name", charity.getString("charityName"));

                // Handle missing website URL
                String website = charity.optString("url", "");
                simplifiedCharity.put("website", website.isEmpty() ? "No website available" : website);

                // Create location string
                String location = charity.getString("city") + ", " + charity.getString("state");
                simplifiedCharity.put("location", location);

                simplifiedCharities.put(simplifiedCharity);
            }

            jsonResponse.put("charities", simplifiedCharities);

            // Send response
            try (PrintWriter out = response.getWriter()) {
                out.print(jsonResponse.toString());
            }

        } catch (Exception e) {
            sendErrorResponse(response, "Error fetching charity data: " + e.getMessage());
        }
    }

    private JSONArray fetchCharitiesFromAPI(String category, String city) throws IOException {
        // Encode parameters for URL
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());

        // Build URL
        String apiUrl = API_BASE_URL +
                "?user_key=" + API_KEY +
                "&category=" + category +
                "&city=" + encodedCity;

        // Create HTTP client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);

            // Execute request
            try (CloseableHttpResponse apiResponse = httpClient.execute(request)) {
                HttpEntity entity = apiResponse.getEntity();

                if (entity != null) {
                    // Convert response to string
                    String result = EntityUtils.toString(entity);
                    JSONObject jsonResponse = new JSONObject(result);

                    // Check if API call was successful
                    if (!jsonResponse.getString("code").equals("200")) {
                        throw new IOException("API Error: " + jsonResponse.optString("msg", "Unknown error"));
                    }

                    // Return data array
                    return jsonResponse.getJSONArray("data");
                } else {
                    throw new IOException("Empty response from API");
                }
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("success", false);
        errorResponse.put("message", message);

        try (PrintWriter out = response.getWriter()) {
            out.print(errorResponse.toString());
        }
    }
}