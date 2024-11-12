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
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
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
        } catch (IOException | ClassNotFoundException | SQLException | InterruptedException e) {
            Message message = new Message(
                    -1,
                    "error",
                    null,
                    e.getMessage()
            );

            sendSocketMessage(out, message);

            System.out.println("Client disconnected: " +  clientSocket.getRemoteSocketAddress());
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
            case "invite":
                handleInvite(message);
                break;
            case "invite accepted":
                handleInviteResponse(message);
                break;
            case "invite declined":
                handleInviteDecline(message);
                break;
            case "new round":
                handleNewRound(message);
                break;
            case "answer":
                handleAnswer(message);
                break;
            case "end":
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
            case "play audio":
                handlePlayAudio(message);
                break;
        }
    }

    private void handlePlayAudio(Message message) throws SQLException {
        int userId = message.getSender();
        User user = userDAO.getUser(userId);

        DataOutputStream out = clients.get(user);

        String path = message.getData();
        File audioFile = new File(path);

        if (audioFile.exists() && audioFile.isFile()) {
            System.out.println("Tìm thấy file: " + path);

            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
                System.out.println("Gửi file hoàn tất!");

            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi gửi file âm thanh.", e);
            }
        } else {
            System.out.println("Không tìm thấy file: " + path);
        }
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

    // xu li khi nguoi dung gui cau tra loi
    private synchronized void handleAnswer(Message message) throws InterruptedException, SQLException, IOException, ClassNotFoundException {
        AnswerRequest request = Parser.fromJson(message.getData(), AnswerRequest.class);
        int userId = message.getSender();
        int matchId = request.getMatchId();
        int round = request.getRound();

        synchronized (answers) {
            if (answers.containsKey(matchId)) {
                AnswerRequest oppRequest = Parser.fromJson(answers.get(matchId).getData(), AnswerRequest.class);
                int oppId = answers.get(matchId).getSender();

                float time1 = request.getTime();
                float time2 = oppRequest.getTime();

                Question question = questionDAO.getQuestionsByRoundAndMatch(matchId, round);
                String answer1 = request.getAnswer();
                String answer2 = oppRequest.getAnswer();

                int winnerId = 0;
                int loserId = 0;

                if (answer1.equals(question.getAnswer()) && !answer2.equals(question.getAnswer())) {
                    winnerId = userId;
                    loserId = oppId;
                    handleWinOrLose(winnerId, loserId, question.getAnswer());
                } else if (answer2.equals(question.getAnswer()) && !answer1.equals(question.getAnswer())) {
                    winnerId = oppId;
                    loserId = userId;
                    handleWinOrLose(winnerId, loserId, question.getAnswer());
                } else {
                    if (time1 < time2) {
                        winnerId = userId;
                        loserId = oppId;
                    } else if (time2 < time1) {
                        winnerId = oppId;
                        loserId = userId;
                    } else {
                        handleDraw(userId, oppId);
                        return;
                    }
                    handleCompareTime(winnerId, loserId, time1, time2);
                }

                answers.remove(matchId);
            } else {
                answers.put(matchId, message);
            }
        }
    }

    private void handleEndgame(Message message) throws SQLException {
        EndRequest request = Parser.fromJson(message.getData(), EndRequest.class);

        int userId = message.getSender();

        userDAO.updateUserMatch(userId, request.getMatchId(), request.getFinalScore());

        playing.remove(userId);
    }

    private void handleDraw(int userId1, int userId2) throws SQLException, IOException, ClassNotFoundException {
        User user1 = userDAO.getUser(userId1);
        User user2 = userDAO.getUser(userId2);

        DataOutputStream out1 = clients.get(user1);
        DataOutputStream out2 = clients.get(user2);

        Message response = new Message(
                -1,
                "answer",
                "Draw!",
                null
        );

        sendSocketMessage(out1, response);
        sendSocketMessage(out2, response);
    }

    private void handleWinOrLose(int winnerId, int loserId, String correctAnswer) throws SQLException, IOException, ClassNotFoundException {
        User winner = userDAO.getUser(winnerId);
        User loser = userDAO.getUser(loserId);

        DataOutputStream out1 = clients.get(winner);
        DataOutputStream out2 = clients.get(loser);

        Message winnerMsg = new Message(
                -1,
                "answer",
                correctAnswer,
                "You win!!"
        );

        Message loserMsg = new Message(
                -1,
                "answer",
                correctAnswer,
                "Wrong answer"
        );

        sendSocketMessage(out1, winnerMsg);
        sendSocketMessage(out2, loserMsg);
    }

    private void handleCompareTime(int winnerId, int loserId, float winnerTime, float loserTime) throws SQLException, IOException, ClassNotFoundException {
        User winner = userDAO.getUser(winnerId);
        User loser = userDAO.getUser(loserId);

        DataOutputStream out1 = clients.get(winner);
        DataOutputStream out2 = clients.get(loser);

        Message winnerMsg = new Message(
                -1,
                "answer",
                "You win your opponent time is " + loserTime,
                null
        );

        Message loserMsg = new Message(
                -1,
                "answer",
                "You lose, your opponent time is " + winnerTime,
                null
        );

        sendSocketMessage(out1, winnerMsg);
        sendSocketMessage(out2, loserMsg);
    }

    // xu li khi bat dau 1 vong choi moi
    private void handleNewRound(Message message) throws SQLException, IOException, ClassNotFoundException {
        RoundRequest request = Parser.fromJson(message.getData(), RoundRequest.class);

        int userId = request.getUserId();
        User user = userDAO.getUser(userId);

        int round = request.getRound();
        int matchId = request.getMatchId();

        Question question = questionDAO.getQuestionsByRoundAndMatch(matchId, round);

        Message response = new Message(
                -1,
                "new round",
                Parser.toJson(question),
                null
        );

        DataOutputStream out = clients.get(user);
        sendSocketMessage(out, response);
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
        Message response = new Message(
                -1,
                "invite accepted",
                Parser.toJson(match),
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
            Message message = new Message(-1, "online", Parser.toJson(users), null);
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
