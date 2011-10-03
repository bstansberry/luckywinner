package org.jboss.as.demos.luckywinner.webapp;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(name = "LuckyWinnerServlet", urlPatterns = {"/simple", "/other", "/enter"})
public class LuckyWinnerServlet extends HttpServlet {

    private static final long serialVersionUID = -2579304186167063651L;

    Logger log = Logger.getLogger(LuckyWinnerServlet.class.getName());

    private static volatile boolean initialized;
    @Resource(lookup="java:jboss/datasources/ExampleDS") DataSource ds;
    @Resource(lookup="java:/ConnectionFactory") QueueConnectionFactory cf;
    @Resource(lookup="java:/queue/demo") Queue queue;
    QueueConnection conn;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String value = req.getParameter("value");

        Writer writer = resp.getWriter();
        writer.write("<html><body>");

        if (value != null) {
            writer.write("Thank you for your submission!\n");

            writer.write("So far we have received entries from:<br>\n");

            Connection sqlConn = null;
            PreparedStatement stmt = null;
            try {
                sqlConn = this.ds.getConnection();
                stmt = sqlConn.prepareCall("INSERT INTO WebAppTestTable (value) VALUES ('" + value + "')");
                stmt.executeUpdate();
            } catch (Exception e) {
                throw new ServletException(e);
            } finally {
                safeClose(stmt);
                safeClose(sqlConn);
            }

            ResultSet rs = null;
            try {
                sqlConn = this.ds.getConnection();
                stmt = sqlConn.prepareStatement("select * from WebAppTestTable");
                rs = stmt.executeQuery();
                while (rs.next()) {
                    writer.write(rs.getInt(1));
                    writer.write(" - ");
                    writer.write(rs.getString(2) + "<br>\n");
                }
            } catch (Exception e) {
                throw new ServletException(e);
            } finally {
                safeClose(rs);
                safeClose(stmt);
                safeClose(sqlConn);
            }

            QueueSession session = null;
            try {
                session = this.conn.createQueueSession(false, 1);
                QueueSender sender = session.createSender(this.queue);
                TextMessage msg = session.createTextMessage(value);

                sender.send(msg);
            } catch (JMSException e) {
                throw new ServletException(e);
            } finally {
                try {
                    if (session != null)
                        session.close();
                } catch (Exception localException1) {
                }
            }
            writer.write("<p>You are welcome to enter as many times as you like!");
        }
        writer.write("<form method='get' action='simple''>Please enter your name to enter our cash drawing: <input type=text name=value><br>");
        writer.write("<input type=submit></form></body></html>");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    public void init() throws ServletException {
        if (!initialized) {
            synchronized (this) {
                if (initialized) {
                    return;
                }
                initialized = true;
                try {
                    //InitialContext context = new InitialContext();
                    //this.ds = ((DataSource) context.lookup("java:/H2DS"));
                    //this.cf = ((QueueConnectionFactory) context.lookup("java:/ConnectionFactory"));
                    //this.queue = ((Queue) context.lookup("queue/demo"));
                    this.conn = this.cf.createQueueConnection();
                } catch (Exception e) {
                    throw new ServletException(e);
                }

                Connection sqlConn = null;
                CallableStatement stmt = null;
                try {
                    sqlConn = this.ds.getConnection();
                    stmt = sqlConn.prepareCall("CREATE TABLE WebAppTestTable (id INTEGER IDENTITY, value VARCHAR(255))");
                    stmt.execute();
                } catch(Exception e) {
                    throw new ServletException(e);

                } finally {
                    safeClose(stmt);
                    safeClose(sqlConn);
                }
                this.log.info("Created table");
            }
        }
    }

    public void destroy() {
        Connection sqlConn = null;
        CallableStatement stmt = null;
        try {
            sqlConn = this.ds.getConnection();
            this.log.info("Dropping table");
            stmt = sqlConn.prepareCall("DROP TABLE WebAppTestTable");
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            safeClose(stmt);
            safeClose(sqlConn);
        }
        try {
            this.conn.stop();
            this.conn.close();
        } catch (JMSException localJMSException) {
        }
    }

    private void safeClose(Statement c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Exception localException) {
        }
    }

    private void safeClose(Connection c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Exception localException) {
        }
    }

    private void safeClose(ResultSet c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Exception localException) {
        }
    }
}
