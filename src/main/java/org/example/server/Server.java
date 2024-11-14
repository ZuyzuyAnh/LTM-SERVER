package org.example.server;

import com.mysql.cj.xdevapi.Client;
import org.example.dao.MatchDAO;
import org.example.dao.QuestionDAO;
import org.example.dao.UserDAO;
import org.example.dto.*;
import org.example.model.Match;
import org.example.model.Question;
import org.example.model.User;

import java.io.*;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

// Server de tiep nhan Message tu client
public class Server {

    // toan bo se su dung ConcurrentHashMap de doc ghi bat dong bo, tranh bi data race
    // (canh tranh tai nguyen khi 2 client ghi vao map cung 1 luc)

    // danh sach cac nguoi choi online duoc map den cac ket noi.
    // key trong map se id cua user, value la ket noi.
    private final ConcurrentHashMap<User, DataOutputStream> clients = new ConcurrentHashMap<>();

    // danh sach nguoi choi dang duoc moi
    // key la id cua nguoi moi. value la danh sach cac nguoi duoc moi
    private final ConcurrentHashMap<Integer, List<Integer>> pendings = new ConcurrentHashMap<>();

    // danh sach nguoi choi dang in game (khong co value, chi co key la user)
    private final ConcurrentHashMap<User, Integer> playing = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, Message> answers = new ConcurrentHashMap<>();

    private Map<Integer, MatchScore> matchScores = new HashMap<>(); // key: matchId

    private final UserDAO userDAO;
    private final MatchDAO matchDAO;
    private final QuestionDAO questionDAO;

    public Server(UserDAO userDAO, MatchDAO matchDAO, QuestionDAO questionDAO) {
        this.userDAO = userDAO;
        this.matchDAO = matchDAO;
        this.questionDAO = questionDAO;
    }

    // server chay va lang nghe tren port
    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                // moi 1 user ket noi den thi mo mot thread de lang nghe thong diep tu nguoi choi do
                new Thread(() -> {
                    try {
                        while (true) {
                            handleClient(clientSocket);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }catch (BindException _) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // xu li cac message tu client
    private void handleClient(Socket clientSocket) throws IOException, ClassNotFoundException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        try {
            Message message = receiveSocketMessage(in);

            // gui ca client socket cho action login (truong hop nguoi dung sai thong tin login)
            handleAction(message, clientSocket);
        } catch (SQLException e) {
            Message message = new Message(
                    -1,
                    "error",
                    null,
                    e.getMessage()
            );

            sendSocketMessage(out, message);

            System.out.println("Client disconnected: " +  clientSocket.getRemoteSocketAddress());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // lay ra hanh dong cua user de xu li
    private void handleAction(Message message, Socket clientSocket) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
        String action = message.getAction();

        switch (action) {
            case "signup":
                handleSignUp(message, clientSocket);
                break;
            case "login":
                handleLogin(message, clientSocket);
                break;
            case "logout":
                handleLogout(message);
                break;
            case "invite":
                handleInvite(message);
                break;
            case "invite accepted":
                handleInviteResponse(message);
                break;
            case "invite declined":
                handleInviteDecline(message);
                break;
            case "end game":
                handleEndgame(message);
                break;
            case "history":
                handleHistory(message);
                break;
            case "leaderboard":
                handleLeaderBoard(message);
                break;
            case "exit":
                handleExit(message, clientSocket);
                break;
            case "upload audio":
                handleUploadAudio(message);
                break;
            case "questions":
                handleQuestions(message);
                break;
            case "delete question":
                handleDeleteQuestion(message);
                break;
            case "update question":
                handleUpdateQuestion(message);
                break;
            case "update info":
                handleUpdateUserInfo(message);
                break;
        }
    }

    private void handleUpdateUserInfo(Message message) throws SQLException, IOException, ClassNotFoundException {
        User user = Parser.fromJson(message.getData(), User.class);

        userDAO.updateUser(user.getId(), user.getUsername(), user.getEmail(), user.getScore());

        DataOutputStream out = clients.get(user);

        clients.remove(user);

        clients.put(user, out);

        broadcastOnline();
    }


    private void handleUpdateQuestion(Message message) throws SQLException {
        Question question = Parser.fromJson(message.getData(), Question.class);
        questionDAO.updateQuestion(question.getId(), question.getSoundUrl(), question.getAnswer());
    }

    private void handleDeleteQuestion(Message message) throws SQLException {
        int questionId = Parser.fromJson(message.getData(), Integer.class);
        questionDAO.deleteQuestion(questionId);
    }

    private void handleQuestions(Message message) throws SQLException, IOException, ClassNotFoundException {
        List<Question> questions = questionDAO.getAllQuestions();

        int userId = message.getSender();

        User user = userDAO.getUser(userId);

        DataOutputStream out = clients.get(user);

        Message response = new Message(
                -1,
                "questions",
                Parser.toJson(questions),
                null
        );

        sendSocketMessage(out, response);
    }

    private void handleUploadAudio(Message message) throws SQLException {
        Question question = Parser.fromJson(message.getData(), Question.class);

        questionDAO.createQuestion(question.getSoundUrl(), question.getAnswer());

    }

    private void handleLogout(Message message) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        User user = userDAO.getUser(userId);

        if (userId != 0) {
            clients.remove(user);
            broadcastOnline();
        }
    }

    private void handleExit(Message message, Socket client) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        User user = userDAO.getUser(userId);

        if (userId != 0) {
            clients.remove(user);
            broadcastOnline();
        }

        client.close();
    }

    private void handleLeaderBoard(Message message) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        User user = userDAO.getUser(userId);

        List<User> leaderBoard = userDAO.getUsers();

        Message response = new Message(
                -1,
                "leaderboard",
                Parser.toJson(leaderBoard),
                null
        );

        DataOutputStream out = clients.get(user);
        sendSocketMessage(out, response);
    }

    private void handleHistory(Message message) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        User user = userDAO.getUser(userId);

        List<HistoryResponse> history = userDAO.getUserMatchHistory(userId);

        Message response = new Message(
                -1,
                "history",
                Parser.toJson(history),
                null
        );

        DataOutputStream out = clients.get(user);
        sendSocketMessage(out, response);
    }

    private void handleEndgame(Message message) throws SQLException, IOException, ClassNotFoundException {
        EndRequest request = Parser.fromJson(message.getData(), EndRequest.class);
        int userId = message.getSender();
        int matchId = request.getMatchId();
        int score = request.getFinalScore();

        User user = userDAO.getUser(userId);
        User opponent = userDAO.findOpponent(matchId, userId);
        MatchScore matchScore = matchScores.computeIfAbsent(matchId, id -> new MatchScore(userId, opponent.getId()));

        matchScore.setScore(userId, score);

        DataOutputStream userOut = clients.get(user);
        DataOutputStream oppOut = clients.get(opponent);

        userDAO.updateUserMatchScore(userId, matchId, score);

        if (matchScore.isBothScoresSubmitted()) {
            int winnerId = matchScore.getWinnerId();

            if (winnerId == -1) {
                user.setScore(user.getScore()+1);
                opponent.setScore(opponent.getScore()+1);
                userDAO.updateUser(user.getId(), user.getUsername(), user.getEmail(), user.getScore());
                userDAO.updateUser(opponent.getId(), opponent.getUsername(), opponent.getEmail(), opponent.getScore());

                Message response = new Message(
                        -1,
                        "end game",
                        "Tie!",
                        null
                );

                sendSocketMessage(userOut, response);
                sendSocketMessage(oppOut, response);
            } else {
                Message winResponse = new Message(
                        -1,
                        "end game",
                        "You win",
                        null
                );

                Message loseResponse = new Message(
                        -1,
                        "end game",
                        "You lose",
                        null
                );

                if (winnerId == userId) {
                    user.setScore(user.getScore()+3);
                    userDAO.updateUser(user.getId(), user.getUsername(), user.getEmail(), user.getScore());

                    sendSocketMessage(userOut, winResponse);
                    sendSocketMessage(oppOut, loseResponse);
                }else {
                    opponent.setScore(opponent.getScore()+3);
                    userDAO.updateUser(opponent.getId(), opponent.getUsername(), opponent.getEmail(), opponent.getScore());

                    sendSocketMessage(userOut, loseResponse);
                    sendSocketMessage(oppOut, winResponse);
                }
            }

            matchScores.remove(matchId);

            clients.remove(opponent);
            clients.remove(user);

            clients.put(user, userOut);
            clients.put(opponent, oppOut);

            playing.remove(user);
            playing.remove(opponent);
        }

        userDAO.updateUserMatch(userId, matchId, score);


        broadcastPlaying();
    }

    // xu li phan hoi loi moi khong duoc chap nhan
    private void handleInviteDecline(Message message) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        int invitedId = Parser.fromJson(message.getData(), Integer.class);

        User invited = userDAO.getUser(invitedId);

        // xoa nguoi duoc moi khoi danh sach moi cua nguoi moi
        List<Integer> userInviteList = pendings.get(userId);
        if (userInviteList != null) {
            userInviteList.remove(invitedId);
        }

        Message response = new Message(
                -1,
                "invite declined",
                invited + " declined your invitation",
                null
        );

        DataOutputStream out = clients.get(invited);

        sendSocketMessage(out, response);
    }

    // xu li phan hoi loi moi duoc chap nhan
    private void handleInviteResponse(Message message) throws SQLException, IOException, ClassNotFoundException {
        int invitedId = message.getSender();
        int userId = Parser.fromJson(message.getData(), Integer.class);

        User invited = userDAO.getUser(invitedId);
        User user = userDAO.getUser(userId);

        //Them vao danh sach in game
        playing.putIfAbsent(user, 0);
        playing.putIfAbsent(invited, 0);

        // xoa tat ca danh sach moi cua nguoi moi
        List<Integer> userInviteList = pendings.get(userId);
        if (userInviteList != null) {
            userInviteList.clear();
        }

        // xoa tat ca danh sach moi cua nguoi duoc moi
        List<Integer> invitedInviteList = pendings.get(userId);
        if (invitedInviteList != null) {
            invitedInviteList.clear();
        }

        // Xoa 2 nguoi choi khoi danh sach moi cua cac nguoi choi khac
        for (Map.Entry<Integer, List<Integer>> entry: pendings.entrySet()) {
            List<Integer> pendingList = entry.getValue();

            if (pendingList.contains(userId)) {
                pendingList.remove(userId);
            }

            if (pendingList.contains(invitedId)) {
                pendingList.remove(invitedId);
            }
        }

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        List<Question> randomQuestions = questionDAO.getRandomQuestions();

        Match match = matchDAO.createMatch(currentTime, userId, invitedId, randomQuestions);
        match.setUser1(user);
        match.setUser2(invited);

        List<QuestionAnswer> questionAnswers = questionDAO.getQuestionsByMatch(match.getId());

        MatchStart matchStart = new MatchStart(
                match,
                questionAnswers
        );

        Message response = new Message(
                -1,
                "invite accepted",
                Parser.toJson(matchStart),
                null
        );

        DataOutputStream invitedOut = clients.get(invited);
        DataOutputStream userOut = clients.get(user);

        broadcastPlaying();

        sendSocketMessage(invitedOut, response);
        sendSocketMessage(userOut, response);
    }

    // Xu li moi nguoi choi
    private void handleInvite(Message message) throws SQLException, IOException, ClassNotFoundException {
        int userId = message.getSender();
        int invitedId = Parser.fromJson(message.getData(), Integer.class);

        User user = userDAO.getUser(userId);
        User invited = userDAO.getUser(invitedId);

        DataOutputStream userIn =  clients.get(user);
        DataOutputStream invitedIn = clients.get(invited);

        Message response = null;
        if (pendings.containsKey(userId) && pendings.get(userId).contains(invitedId)) {
            String errMessage = "User " + invited.getUsername() + " invited, please wait";

            response = new Message(
                    invitedId,
                    "invite",
                    null,
                    errMessage
            );

            // neu nguoi choi da duoc moi, thi gui lai thong bao cho nguoi moi
            sendSocketMessage(userIn, response);
        }else {
            response = new Message(
                    userId,
                    "invite",
                    user.getUsername(),
                    null
            );

            // neu nguoi choi chua duoc moi, gui thong bao moi cho nguoi choi
            sendSocketMessage(invitedIn, response);
        }
    }

    // Xu li dang ki
    private void handleSignUp(Message message, Socket clientSocket) throws IOException, ClassNotFoundException, SQLException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        SignupRequest request = Parser.fromJson(message.getData(), SignupRequest.class);

        Message response = null;

        User user = userDAO.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                "player"
        );

        response = new Message(
                -1,
                "signup",
                "Signup successfully",
                null
        );

        sendSocketMessage(out, response);
    }

    // Xu li dang nhap
    private void handleLogin(Message message, Socket clientSocket) throws IOException, ClassNotFoundException, SQLException {
        DataOutputStream in = new DataOutputStream(clientSocket.getOutputStream());
        LoginRequest request = Parser.fromJson(message.getData(), LoginRequest.class);
        String username = request.getUsername();

        User user = userDAO.getUserByName(username);

        if (!clients.containsKey(user) && user.getPassword().equals(request.getPassword())) {
            Message response = new Message(
                    -1,
                    "login",
                    Parser.toJson(user),
                    null
            );

            sendSocketMessage(in, response);

            clients.put(user, in);

            if (user.getRole().equals("admin")) {
                return;
            }

            broadcastOnline();
        } else if (clients.containsKey(user)) {
            String errMessage = "User is already online";
            Message response = new Message(
                    -1,
                    "login",
                    null,
                    errMessage
            );

            sendSocketMessage(in, response);
        } else {
            String errMessage = "Wrong username or password";
            Message response = new Message(
                    -1,
                    "login",
                    null,
                    errMessage
            );

            sendSocketMessage(in, response);
        }
    }

    // broadcast trang thai online den tat ca nguoi choi, thuong dung khi co nguoi choi login vao
    public void broadcastOnline() throws IOException, ClassNotFoundException {
        User[] users = clients.keySet().toArray(new User[0]);

        for (Map.Entry<User, DataOutputStream> entry : clients.entrySet()) {
            Message message = new Message(-1, "online", Parser.toJson(users), null);
            sendSocketMessage(entry.getValue(), message);
        }
    }

    public void broadcastPlaying() throws IOException, ClassNotFoundException {
        User[] users = playing.keySet().toArray(new User[0]);

        for (Map.Entry<User, DataOutputStream> entry : clients.entrySet()) {
            Message message = new Message(-1, "playing", Parser.toJson(users), null);
            sendSocketMessage(entry.getValue(), message);
        }
    }

    // Nhan message tu data input stream cua client.
    private Message receiveSocketMessage(DataInputStream in) throws IOException, ClassNotFoundException {
        return Parser.fromJson(in.readUTF(), Message.class);
    }

    // Gui message den cho user
    private void sendSocketMessage(DataOutputStream out, Message msg) throws IOException, ClassNotFoundException {
        out.writeUTF(Parser.toJson(msg));
    }
}
