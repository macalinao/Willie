package com.drtshock.willie.github;
import org.pircbotx.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GistHelper {

    private static final String GITHUB_API_URL    = "https://api.github.com/";
    private static final String GIST_API_LOCATION = "gists";
    private static final String GIST_URL          = GITHUB_API_URL + GIST_API_LOCATION;

    private static final String DESCRIPTION = "Willie pasted this on ";

    private static final Logger LOG = Logger.getLogger(GistHelper.class.getName());

    /**
     * Paste a String to Gist then returns the link to the gist
     *
     * @param toGist the String to paste
     *
     * @return the link to the paste
     */
    public static String gist(String toGist, /* DEBUG */ User user) {
        OutputStream out = null;
        InputStream in = null;
        try {
            URL url = new URL(GIST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("content-type", "application/json; charset=utf-8");

            GistCreationJsonRequest req = new GistCreationJsonRequest();
            req.description = DESCRIPTION + date();
            req.isPublic = true;
            GistCreationJsonRequestFile file = new GistCreationJsonRequestFile();
            file.fileName = "WilliePaste-" + date().replace(' ', '-');
            file.fileContent = toGist;
            req.files = new GistCreationJsonRequestFile[] {file};
            String jsonString = req.toJsonString();

            connection.setRequestProperty("Content-Length", Integer.toString(jsonString.length()));

            connection.connect();

            out = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write(req.toJsonString());
            writer.flush();
            writer.close();

            in = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in));
            String line, response = "";
            while ((line = rd.readLine()) != null) {
                response = response + line;
            }
            rd.close(); //close the reader

            return response;
        } catch (IOException e) {

            // ### DEBUG ### //
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String stackTrace = writer.toString();
            user.sendMessage(stackTrace);
            // ############# //

            LOG.severe("Failed to Gist, error follows:");
            LOG.log(Level.SEVERE, e.getMessage(), e);
            LOG.severe("This is what I was trying to Gist:");
            LOG.severe("\n##########" + toGist + "\n##########");
            LOG.severe("Failed to Gist, error above.");
            return "ERROR";
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOP
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOP
                }
            }
        }
    }

    private static String date() {
        return new SimpleDateFormat("EEEE dd MMMM YYYY").format(new Date());
    }

    private static class GistCreationJsonRequest {

        public String                        description;
        public boolean                       isPublic;
        public GistCreationJsonRequestFile[] files;

        public String toJsonString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("{\"description\":\"");
            builder.append(description);
            builder.append("\",\"public\":");
            builder.append(isPublic);
            builder.append(",\"files\":{");
            for (int i = 0; i < files.length; i++) {
                GistCreationJsonRequestFile file = files[i];
                builder.append('"');
                builder.append(file.fileName);
                builder.append("\":{\"content\":\"");
                builder.append(escape(file.fileContent));
                builder.append("\"}");
                if (i != files.length - 1) {
                    builder.append(',');
                }
            }
            builder.append("}}");
            return builder.toString();
        }
    }

    private static class GistCreationJsonRequestFile {

        public String fileName;
        public String fileContent;
    }

    /** Escape a String for JSON */
    private static String escape(String s) {
        final StringBuilder sb = new StringBuilder();
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}