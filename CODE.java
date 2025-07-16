import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PersonalTaskManagerViolations {

    private static final String DB_FILE_PATH = "tasks_database.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private boolean isValidPriority(String priorityLevel) {
        return priorityLevel.equals("Thấp") || priorityLevel.equals("Trung bình") || priorityLevel.equals("Cao");
    }

    private boolean isDuplicateTask(JSONArray tasks, String title, String dueDateStr) {
        for (Object obj : tasks) {
            JSONObject existingTask = (JSONObject) obj;
            if (existingTask.get("title").toString().equalsIgnoreCase(title) &&
                existingTask.get("due_date").toString().equals(dueDateStr)) {
                return true;
            }
        }
        return false;
    }

    private boolean validateInputs(String title, String dueDateStr, String priorityLevel) {
        if (title == null || title.trim().isEmpty()) {
            System.out.println("Lỗi: Tiêu đề không được để trống.");
            return false;
        }
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            System.out.println("Lỗi: Ngày đến hạn không được để trống.");
            return false;
        }
        try {
            LocalDate.parse(dueDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.out.println("Lỗi: Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
            return false;
        }
        if (!isValidPriority(priorityLevel)) {
            System.out.println("Lỗi: Mức độ ưu tiên không hợp lệ. Vui lòng chọn từ: Thấp, Trung bình, Cao.");
            return false;
        }
        return true;
    }

    private static JSONArray loadTasksFromDb() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(DB_FILE_PATH)) {
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (IOException | ParseException e) {
            System.err.println("Lỗi khi đọc file database: " + e.getMessage());
        }
        return new JSONArray();
    }

    private static void saveTasksToDb(JSONArray tasksData) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) {
            file.write(tasksData.toJSONString());
            file.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage());
        }
    }

    public JSONObject addNewTaskWithViolations(String title, String description,
                                               String dueDateStr, String priorityLevel,
                                               boolean isRecurring) {

        if (!validateInputs(title, dueDateStr, priorityLevel)) return null;

        LocalDate dueDate = LocalDate.parse(dueDateStr, DATE_FORMATTER);
        JSONArray tasks = loadTasksFromDb();

        if (isDuplicateTask(tasks, title, dueDateStr)) {
            System.out.println("Lỗi: nhiệm vụ đã tồn tại.");
            return null;
        }

        JSONObject task = createTaskObject(title, description, dueDate, priorityLevel);
        tasks.add(task);
        saveTasksToDb(tasks);

        System.out.println("Đã thêm nhiệm vụ thành công: " + task.get("id"));
        return task;
    }

    private JSONObject createTaskObject(String title, String description, LocalDate dueDate, String priorityLevel) {
        JSONObject newTask = new JSONObject();
        newTask.put("id", UUID.randomUUID().toString());
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("due_date", dueDate.format(DATE_FORMATTER));
        newTask.put("priority", priorityLevel);
        newTask.put("status", "Chưa hoàn thành");
        newTask.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return newTask;
    }

    public static void main(String[] args) {
        PersonalTaskManagerViolations manager = new PersonalTaskManagerViolations();

        System.out.println("\n✅ Thêm nhiệm vụ hợp lệ:");
        manager.addNewTaskWithViolations("Mua sách", "Sách Công nghệ phần mềm.", "2025-07-20", "Cao", false);

        System.out.println("\n⚠️ Thêm nhiệm vụ trùng lặp:");
        manager.addNewTaskWithViolations("Mua sách", "Sách Công nghệ phần mềm.", "2025-07-20", "Cao", false);

        System.out.println("\n✅ Thêm nhiệm vụ hợp lệ khác:");
        manager.addNewTaskWithViolations("Tập thể dục", "Tập gym 1 tiếng.", "2025-07-21", "Trung bình", true);

        System.out.println("\n❌ Thêm nhiệm vụ không có tiêu đề:");
        manager.addNewTaskWithViolations("", "Không có tiêu đề.", "2025-07-22", "Thấp", false);

        System.out.println("\n❌ Thêm nhiệm vụ sai định dạng ngày:");
        manager.addNewTaskWithViolations("Nấu ăn", "Nấu bữa tối.", "22-07-2025", "Cao", false);

        System.out.println("\n❌ Thêm nhiệm vụ sai mức ưu tiên:");
        manager.addNewTaskWithViolations("Học bài", "Ôn thi giữa kỳ.", "2025-07-23", "Rất cao", false);
    }
}
