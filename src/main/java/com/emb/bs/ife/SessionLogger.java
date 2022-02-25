package com.emb.bs.ife;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

class SessionLogger extends ArrayList<String> {

    boolean doIt = true;
    private ArrayList<JsonNode> req = new ArrayList<>(5000);

    private static final Logger iLOG = LoggerFactory.getLogger(Session.class);

    public void debug(String s) {
        if (doIt) {
            if (Snake.loggingFailed) {
                add(s);
            }
            iLOG.info(s);
        }
    }

    public void info(String s) {
        if (doIt) {
            if (Snake.loggingFailed) {
                add(s);
            }
            iLOG.info(s);
        }
    }

    public void info(String s, Throwable t) {
        if (doIt) {
            if (Snake.loggingFailed) {
                add(s);
            }
            iLOG.info(s, t);
        }
    }

    public void error(String s) {
        if (doIt) {
            if (Snake.loggingFailed) {
                add(s);
            }
            iLOG.error(s);
        }
    }

    void write() {
        if (Snake.loggingFailed) {
            Long ts = System.currentTimeMillis();
            writeLOG(ts);
            writeREQ(ts);
        }
    }

    private void writeLOG(long ts) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File("log_" + ts + ".txt"));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : this) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            iLOG.error("", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void writeREQ(long ts) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File("req_" + ts + ".txt"));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (JsonNode json : req) {
                bw.write(json.toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            iLOG.error("", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
    }

    void logReq(JsonNode json) {
        req.add(json);
    }
}
