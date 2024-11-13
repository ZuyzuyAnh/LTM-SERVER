package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.dao.MatchDAO;
import org.example.dao.QuestionDAO;
import org.example.dao.UserDAO;
import org.example.server.Server;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("HTTP server started at http://localhost:8081");
        MatchDAO matchDAO = new MatchDAO();
        QuestionDAO questionDAO = new QuestionDAO();
        UserDAO userDAO = new UserDAO();
        Server server = new Server(userDAO, matchDAO, questionDAO);
        server.start(8080);
    }
}
