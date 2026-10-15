import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

/** QuizCraft Review 1 prototype: Java HTTP server + HTML/CSS/JavaScript client. */
public class QuizCraftServer {
    private static final int PORT = 8090;
    private static final Path ROOT = Paths.get("web").toAbsolutePath().normalize();
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path STORE = DATA_DIR.resolve("quizcraft-store.bin");
    private static Map<String,Object> state;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATA_DIR);
        state = loadState();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", QuizCraftServer::api);
        server.createContext("/", QuizCraftServer::staticFile);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("QuizCraft is running at http://localhost:" + PORT);
        System.out.println("Demo prototype only. Do not expose this server to the public internet.");
    }

    @SuppressWarnings("unchecked")
    private static synchronized Map<String,Object> loadState() {
        if (Files.exists(STORE)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(STORE))) {
                Object o = in.readObject(); if (o instanceof Map) return (Map<String,Object>)o;
            } catch (Exception e) { System.err.println("Could not read store; loading demo data: " + e.getMessage()); }
        }
        Map<String,Object> s = new LinkedHashMap<>();
        List<Map<String,Object>> users = new ArrayList<>();
        users.add(map("id", "U1", "name", "Aarav Sharma", "email", "admin@quizcraft.demo", "role", "Admin", "active", true));
        users.add(map("id", "U2", "name", "Priya Verma", "email", "creator@quizcraft.demo", "role", "Quiz Creator", "active", true));
        users.add(map("id", "U3", "name", "Demo Participant", "email", "participant@quizcraft.demo", "role", "Participant", "active", true));
        List<Map<String,Object>> quizzes = new ArrayList<>();
        quizzes.add(quiz("Q1", "Java Fundamentals", "Core Java syntax, types and control flow", "Priya Verma", 5, "Approved", Arrays.asList(
          question("Which keyword creates a subclass in Java?", "implements", "extends", "inherits", "super", "B", "The extends keyword establishes class inheritance."),
          question("Which is not a primitive Java type?", "int", "boolean", "String", "double", "C", "String is a class, not a primitive type."),
          question("Which method is the usual Java program entry point?", "start()", "run()", "main()", "init()", "C", "The standard entry point is public static void main(String[] args)."),
          question("Which collection does not allow duplicate elements?", "ArrayList", "HashSet", "LinkedList", "Vector", "B", "A Set models unique elements."),
          question("What does JVM stand for?", "Java Variable Method", "Java Verified Machine", "Java Virtual Machine", "Joint Virtual Memory", "C", "JVM means Java Virtual Machine."))));
        quizzes.add(quiz("Q2", "Object-Oriented Programming", "Classes, objects, inheritance and polymorphism", "Priya Verma", 5, "Approved", Arrays.asList(
          question("A class is best described as a:", "Runtime object", "Blueprint for objects", "Loop structure", "Package manager", "B", "A class defines the state and behaviour of objects."),
          question("Which concept hides internal implementation details?", "Encapsulation", "Compilation", "Iteration", "Casting", "A", "Encapsulation bundles state and methods and controls access."),
          question("Method overloading is resolved mainly at:", "Compile time", "Run time only", "Installation", "Link download", "A", "Overloading uses compile-time method selection."),
          question("Which keyword refers to the current object?", "parent", "this", "self", "current", "B", "this refers to the current instance."),
          question("Java supports multiple inheritance of classes directly.", "True", "False", "Only for abstract classes", "Only for final classes", "B", "Java classes have single inheritance; interfaces can be multiple."))));
        quizzes.add(quiz("Q3", "Advanced Java — Draft", "Creator-submitted quiz awaiting admin review", "Priya Verma", 3, "Pending", Arrays.asList(
          question("Which interface represents a task that returns a result?", "Runnable", "Callable", "Serializable", "Cloneable", "B", "Callable.call() can return a result and throw exceptions."))));
        s.put("users", users); s.put("quizzes", quizzes); s.put("attempts", new ArrayList<Map<String,Object>>());
        s.put("messages", new ArrayList<Map<String,Object>>()); s.put("reminders", new ArrayList<Map<String,Object>>());
        s.put("settings", map("platformName", "QuizCraft", "defaultDuration", 10, "passingPercent", 60, "maintenanceMode", false));
        s.put("alerts", new ArrayList<>(Arrays.asList("Demo environment loaded", "Review pending quiz submissions before publishing")));
        saveState(s); return s;
    }
    private static Map<String,Object> question(String q,String a,String b,String c,String d,String correct,String explanation) {
        return map("question",q,"options",Arrays.asList(a,b,c,d),"answer",correct,"explanation",explanation);
    }
    private static Map<String,Object> quiz(String id,String title,String description,String creator,int duration,String status,List<Map<String,Object>> questions) {
        return map("id",id,"title",title,"description",description,"creator",creator,"duration",duration,"status",status,"questions",questions,"createdAt",new Date().toString());
    }
    private static Map<String,Object> map(Object... pairs) { Map<String,Object> m = new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2)m.put(String.valueOf(pairs[i]),pairs[i+1]); return m; }
    private static synchronized void saveState(Map<String,Object> s) { try(ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(STORE))) { out.writeObject(s); } catch(IOException e) { throw new RuntimeException(e); } }
    private static void api(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            String path = ex.getRequestURI().getPath();
            if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && path.equals("/api/state")) { json(ex,200,state); return; }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { json(ex,405,map("error","Method not allowed")); return; }
            Map<String,String> p = parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            synchronized(QuizCraftServer.class) {
                switch(path) {
                    case "/api/quiz/create": createQuiz(p); break;
                    case "/api/quiz/status": setQuizStatus(p); break;
                    case "/api/quiz/delete": deleteQuiz(p); break;
                    case "/api/user/create": createUser(p); break;
                    case "/api/user/toggle": toggleUser(p); break;
                    case "/api/attempt": addAttempt(p); break;
                    case "/api/message": addMessage(p); break;
                    case "/api/reminder": addReminder(p); break;
                    case "/api/settings": updateSettings(p); break;
                    default: json(ex,404,map("error","Unknown API route")); return;
                }
                saveState(state);
            }
            json(ex,200,map("ok",true,"message","Saved successfully","state",state));
        } catch(IllegalArgumentException e) { json(ex,400,map("error",e.getMessage())); }
          catch(Exception e) { e.printStackTrace(); json(ex,500,map("error","Server error: " + e.getMessage())); }
    }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> list(String key) { return (List<Map<String,Object>>)state.get(key); }
    private static String req(Map<String,String> p,String k) { String v=p.getOrDefault(k,"").trim(); if(v.isEmpty()) throw new IllegalArgumentException("Please provide " + k + "."); return v; }
    private static void createQuiz(Map<String,String> p) {
        String title=req(p,"title"), creator=req(p,"creator"), q=req(p,"question"), a=req(p,"a"), b=req(p,"b"), c=req(p,"c"), d=req(p,"d"), answer=req(p,"answer").toUpperCase();
        if(!Arrays.asList("A","B","C","D").contains(answer)) throw new IllegalArgumentException("Correct answer must be A, B, C or D.");
        int duration; try { duration=Integer.parseInt(req(p,"duration")); } catch(NumberFormatException e){throw new IllegalArgumentException("Duration must be a number.");}
        if(duration<1||duration>120) throw new IllegalArgumentException("Duration must be between 1 and 120 minutes.");
        List<Map<String,Object>> qs=new ArrayList<>(); qs.add(question(q,a,b,c,d,answer,p.getOrDefault("explanation","Review the concept and try again.")));
        String id="Q"+(System.currentTimeMillis()%100000000);
        list("quizzes").add(quiz(id,title,p.getOrDefault("description","Java programming quiz"),creator,duration,"Pending",qs));
        ((List<Object>)state.get("alerts")).add("New quiz submitted for review: "+title);
    }
    private static void setQuizStatus(Map<String,String> p) { String id=req(p,"id"), status=req(p,"status"); if(!status.equals("Approved")&&!status.equals("Rejected")&&!status.equals("Pending"))throw new IllegalArgumentException("Invalid status."); for(Map<String,Object> q:list("quizzes"))if(id.equals(q.get("id"))){q.put("status",status);((List<Object>)state.get("alerts")).add("Quiz '"+q.get("title")+"' marked "+status);return;} throw new IllegalArgumentException("Quiz not found."); }
    private static void deleteQuiz(Map<String,String> p) { String id=req(p,"id"); if(!list("quizzes").removeIf(q->id.equals(q.get("id"))))throw new IllegalArgumentException("Quiz not found."); }
    private static void createUser(Map<String,String> p) { String name=req(p,"name"),email=req(p,"email"),role=req(p,"role"); if(!Arrays.asList("Admin","Quiz Creator","Participant").contains(role))throw new IllegalArgumentException("Invalid role."); for(Map<String,Object> u:list("users"))if(email.equalsIgnoreCase(String.valueOf(u.get("email"))))throw new IllegalArgumentException("Email already exists."); list("users").add(map("id","U"+(System.currentTimeMillis()%100000000),"name",name,"email",email,"role",role,"active",true)); }
    private static void toggleUser(Map<String,String> p) { String id=req(p,"id"); for(Map<String,Object> u:list("users"))if(id.equals(u.get("id"))){u.put("active",!Boolean.TRUE.equals(u.get("active")));return;} throw new IllegalArgumentException("User not found."); }
    private static void addAttempt(Map<String,String> p) { String user=req(p,"user"),qid=req(p,"quizId"); int score,total; try{score=Integer.parseInt(req(p,"score"));total=Integer.parseInt(req(p,"total"));}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid score.");} if(total<1||score<0||score>total)throw new IllegalArgumentException("Score is out of range."); String title="Quiz"; for(Map<String,Object> q:list("quizzes"))if(qid.equals(q.get("id"))){title=String.valueOf(q.get("title"));break;} list("attempts").add(map("id","A"+(System.currentTimeMillis()%100000000),"user",user,"quizId",qid,"quizTitle",title,"score",score,"total",total,"percent",Math.round(score*100.0/total),"submittedAt",new Date().toString())); }
    private static void addMessage(Map<String,String> p) { list("messages").add(map("id","M"+(System.currentTimeMillis()%100000000),"sender",req(p,"sender"),"recipient",p.getOrDefault("recipient","Quiz Creator"),"body",req(p,"body"),"createdAt",new Date().toString())); }
    private static void addReminder(Map<String,String> p) { list("reminders").add(map("id","R"+(System.currentTimeMillis()%100000000),"user",req(p,"user"),"text",req(p,"text"),"createdAt",new Date().toString())); }
    private static void updateSettings(Map<String,String> p) { Map<String,Object> s=(Map<String,Object>)state.get("settings"); String name=req(p,"platformName"); int duration;try{duration=Integer.parseInt(req(p,"defaultDuration"));}catch(NumberFormatException e){throw new IllegalArgumentException("Default duration must be a number.");} if(duration<1||duration>120)throw new IllegalArgumentException("Default duration must be 1–120 minutes."); int pass;try{pass=Integer.parseInt(req(p,"passingPercent"));}catch(NumberFormatException e){throw new IllegalArgumentException("Passing percentage must be a number.");}if(pass<0||pass>100)throw new IllegalArgumentException("Passing percentage must be 0–100.");s.put("platformName",name);s.put("defaultDuration",duration);s.put("passingPercent",pass);s.put("maintenanceMode","true".equals(p.get("maintenanceMode"))); }
    private static Map<String,String> parseForm(String body) throws UnsupportedEncodingException { Map<String,String> m=new HashMap<>(); for(String part:body.split("&")){String[] kv=part.split("=",2);m.put(URLDecoder.decode(kv[0],"UTF-8"),kv.length>1?URLDecoder.decode(kv[1],"UTF-8"):"");}return m; }
    private static void json(HttpExchange ex,int code,Object o)throws IOException { byte[] b=toJson(o).getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(code,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);} }
    private static String toJson(Object o) { if(o==null)return "null"; if(o instanceof String)return "\""+escape((String)o)+"\"";if(o instanceof Number||o instanceof Boolean)return String.valueOf(o);if(o instanceof Map){StringJoiner j=new StringJoiner(",","{","}");((Map<?,?>)o).forEach((k,v)->j.add(toJson(String.valueOf(k))+":"+toJson(v)));return j.toString();}if(o instanceof Iterable){StringJoiner j=new StringJoiner(",","[","]");for(Object x:(Iterable<?>)o)j.add(toJson(x));return j.toString();}return toJson(String.valueOf(o)); }
    private static String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t"); }
    private static void staticFile(HttpExchange ex)throws IOException { String path=ex.getRequestURI().getPath();if(path.equals("/"))path="/index.html";Path file=ROOT.resolve(path.substring(1)).normalize();if(!file.startsWith(ROOT)||!Files.exists(file)||Files.isDirectory(file)){byte[] b="Not found".getBytes(StandardCharsets.UTF_8);ex.sendResponseHeaders(404,b.length);try(OutputStream o=ex.getResponseBody()){o.write(b);}return;}String type=file.toString().endsWith(".html")?"text/html; charset=utf-8":file.toString().endsWith(".css")?"text/css; charset=utf-8":file.toString().endsWith(".js")?"application/javascript; charset=utf-8":"application/octet-stream";ex.getResponseHeaders().set("Content-Type",type);byte[] b=Files.readAllBytes(file);ex.sendResponseHeaders(200,b.length);try(OutputStream o=ex.getResponseBody()){o.write(b);} }
}
