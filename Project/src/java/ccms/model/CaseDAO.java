package ccms.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author anita
 */
public class CaseDAO {

    private EmployeeDAO empDAO;
    private PersonDAO personDAO;
    private static final String FILE_NAME = "./data/Holidays2016.txt";
    private static final LinkedHashMap<Integer, String> holidays = new LinkedHashMap<Integer, String>();
    private static final String GET_CASE_BY_EMP_ID = "SELECT c.person_nric, c.reported_date, cc.complaint_case_id, cc.issue, cc.difficulty, cc.add_on_date, max(ch.received_date) as received, ch.last_saved FROM cases c, complaint_case cc, complaint_case_handling ch WHERE c.case_id = cc.complaint_case_id AND cc.complaint_case_id = ch.complaint_case_id AND ch.employee_id = ? AND cc.status NOT IN ('Replied', 'Closed') AND ch.response_date IS NULL group by complaint_case_id ORDER BY ch.received_date DESC";
    private static final String GET_CASE_BY_CASE_ID = "SELECT p.nric, p.email, p.name, p.contact_no, c.description, cc.difficulty, cc.issue, cc.additional_info, cc.add_on_date, ch.response, ch.employee_id, ch.received_date FROM cases c, complaint_case cc, complaint_case_handling ch, person p WHERE ch.complaint_case_id = ? AND ch.received_date IN (SELECT MAX(received_date) AS received FROM complaint_case_handling WHERE response_date IS NULL GROUP BY complaint_case_id) AND c.case_id = cc.complaint_case_id AND cc.complaint_case_id = ch.complaint_case_id AND c.person_nric = p.nric";
    private static final String UPDATE_CASE_RESPONSE = "UPDATE complaint_case_handling SET response = ?, response_date = CAST(? AS DATETIME), last_saved = ? WHERE employee_id = ? AND complaint_case_id = ?";
    private static final String SAVE_CASE_RESPONSE = "UPDATE complaint_case_handling SET response = ?, last_saved = ? WHERE employee_id = ? AND complaint_case_id = ?";
    private static final String GET_CASE_RESPONSES = "SELECT e.position, e.employee_id, e.name, ch.response_date, ch.response FROM complaint_case_handling ch, employee e WHERE e.employee_id = ch.employee_id AND complaint_case_id = ? AND ch.response_date IS NOT NULL ORDER BY ch.response_date DESC";
    private static final String INSERT_TO_CASEHANDLING = "INSERT INTO complaint_case_handling (complaint_case_id, employee_id, received_date, response_date, response, last_saved) VALUES (?, ?, CAST(? AS DATETIME), NULL, NULL, NULL)";
    private static final String UPDATE_CASE_STATUS = "UPDATE complaint_case SET status = ? WHERE complaint_case_id = ?";
    private static final String AUTO_UPDATE_CASE_2_WEEKS = "UPDATE complaint_case SET status = 'Closed' WHERE complaint_case_id IN (SELECT complaint_case_id FROM (SELECT complaint_case_id, MAX(response_date) AS response_date FROM complaint_case_handling WHERE response_date IS NOT NULL GROUP BY complaint_case_id HAVING DATEDIFF(NOW(), response_date) > 14) as temp)";
    private static final String GET_OUTSTANDING_CASES = "SELECT e.on_leave_start, e.on_leave_end, c.reported_date, cc.issue, cc.difficulty, cc.add_on_date, ch.complaint_case_id, ch.employee_id, MAX(received_date) AS received, ch.response, ch.last_saved FROM cases c, complaint_case_handling ch, complaint_case cc, employee e WHERE e.employee_id = ch.employee_id AND c.case_id = cc.complaint_case_id AND cc.complaint_case_id = ch.complaint_case_id AND ch.response_date IS NULL GROUP BY ch.complaint_case_id";
    private static final String INSERT_NEW_CASE = "INSERT INTO cases (description, reported_date, type, recorded_employee_id, person_nric) VALUES (?,CAST(? AS DATETIME),?,?,?)";
    private static final String CASE_COUNT_BY_DIFFICULTY = "SELECT ch.employee_id, cc.difficulty, COUNT(cc.difficulty) AS casecountbydiff FROM complaint_case_handling ch, complaint_case cc WHERE cc.complaint_case_id = ch.complaint_case_id AND ch.received_date IN (SELECT MAX(received_date) AS received FROM complaint_case_handling WHERE response_date IS NULL GROUP BY complaint_case_id) GROUP BY ch.employee_id, cc.difficulty";
    
    public CaseDAO() throws ParseException {
        autoUpdateCaseStatus();
        load();
    }

    public LinkedHashMap<Integer, String> getAllHolidays() {
        return holidays;
    }

    public CaseDAO(EmployeeDAO empDAO, PersonDAO personDAO) throws ParseException {
        this.empDAO = empDAO;
        this.personDAO = personDAO;
        load();
    }

    public static void load() throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

        Calendar calG = new GregorianCalendar();
        //Load list of holidays in 2016 from Holidays2016.txt
        Scanner sc = null;
        try {
            File file = new File(FILE_NAME);
            sc = new Scanner(file);
            sc.useDelimiter("\r\n");

            while (sc.hasNext()) {
                String date = sc.next();
                calG.setTime(dateFormat.parse(date));
                int dayOfYear = calG.get(Calendar.DAY_OF_YEAR);
                holidays.put(dayOfYear, "");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
    }

    public LinkedHashMap<Integer, ArrayList<String>> getCaseCountByDifficulty() throws ParseException {
        LinkedHashMap<Integer, ArrayList<String>> cases = new LinkedHashMap<Integer, ArrayList<String>>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(CASE_COUNT_BY_DIFFICULTY);
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrayList<String> details = new ArrayList<String>();
                int employeeID = Integer.parseInt(rs.getString("employee_id"));
                String difficulty = rs.getString("difficulty");
                String count = difficulty + "_" + rs.getString("casecountbydiff");
                
                if(!cases.containsKey(employeeID)) {                    
                    details.add(count);
                    cases.put(employeeID, details);
                } else {
                    cases.get(employeeID).add(count);
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return cases;
    }
    
    public int updateCaseStatus(String description, String date, String type, int employeeID, String nric) {
        Connection con = null;
        PreparedStatement ps = null;
        int rowUpdate = 0;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(INSERT_NEW_CASE);
            ps.setString(1, description);
            ps.setString(2, date);
            ps.setString(3, type);
            ps.setInt(4, employeeID);
            ps.setString(5, nric);
            rowUpdate = ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rowUpdate;
    }

    public LinkedHashMap<Integer, ArrayList<String>> getOutstandingCases() throws ParseException {
        LinkedHashMap<Integer, ArrayList<String>> cases = new LinkedHashMap<Integer, ArrayList<String>>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(GET_OUTSTANDING_CASES);
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrayList<String> details = new ArrayList<String>();
                int caseID = rs.getInt("complaint_case_id");
                String dateReceived = df.format(rs.getDate("received"));
                String difficulty = rs.getString("difficulty");
                int daysToAdd = 3;
                if (difficulty.equalsIgnoreCase("hard")) {
                    daysToAdd = 5;
                } else if (difficulty.equalsIgnoreCase("super complex")) {
                    daysToAdd = 7;
                }
                String employeeID = rs.getString("employee_id");
                String reportedDate = rs.getString("reported_date");
                String addOnDate = rs.getString("add_on_date");
                String onLeaveStart = rs.getString("on_leave_start");
                String onLeaveEnd = rs.getString("on_leave_end");
                String expectedResponseDate = null;
                if (addOnDate != null) {
                    expectedResponseDate = calculateExpectedResponseDate(14, addOnDate);
                } else {
                    expectedResponseDate = calculateExpectedResponseDate(daysToAdd, reportedDate);
                }
                String issue = rs.getString("issue");
                String lastSaved = rs.getString("last_saved");
                if (lastSaved == null) {
                    lastSaved = "-";
                }
                details.add(dateReceived);
                details.add(expectedResponseDate);
                details.add(difficulty);
                details.add(issue);
                details.add(onLeaveStart);
                details.add(onLeaveEnd);
                details.add(employeeID);
                details.add(lastSaved);
                cases.put(caseID, details);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return cases;
    }

    private void autoUpdateCaseStatus() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(AUTO_UPDATE_CASE_2_WEEKS);
            ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public LinkedHashMap<Integer, ArrayList<String>> getAllCasebyEmpID(int employee_id) throws ParseException {
        LinkedHashMap<Integer, ArrayList<String>> cases = new LinkedHashMap<Integer, ArrayList<String>>();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
//        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(GET_CASE_BY_EMP_ID);
            ps.setInt(1, employee_id);
            rs = ps.executeQuery();

            while (rs.next()) {
                ArrayList<String> details = new ArrayList<String>();
                int caseID = rs.getInt("complaint_case_id");
                String personNRIC = rs.getString("person_nric");
                String dateReceived = sdf2.format(rs.getDate("received"));
                String difficulty = rs.getString("difficulty");
                int daysToAdd = 3;
                if (difficulty.equalsIgnoreCase("hard")) {
                    daysToAdd = 5;
                } else if (difficulty.equalsIgnoreCase("super complex")) {
                    daysToAdd = 7;
                }
                String reportedDate = rs.getString("reported_date");
                String addOnDate = rs.getString("add_on_date");
                String expectedResponseDate = null;
                if (addOnDate != null) {
                    expectedResponseDate = calculateExpectedResponseDate(14, addOnDate);
                } else {
                    expectedResponseDate = calculateExpectedResponseDate(daysToAdd, reportedDate);
                }
                java.util.Date expectedResponseD = sdf1.parse(expectedResponseDate);
                expectedResponseDate = sdf2.format(expectedResponseD);
                String issue = rs.getString("issue");
                String lastSaved = rs.getString("last_saved");
                if (lastSaved == null) {
                    lastSaved = "-";
                } else {
                    java.util.Date lastSavedDate = sdf1.parse(lastSaved);
                    lastSaved = sdf2.format(lastSavedDate);
                }
                details.add(personNRIC);
                details.add(dateReceived);
                details.add(expectedResponseDate);
                details.add(difficulty);
                details.add(issue);
                details.add(lastSaved);
                cases.put(caseID, details);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return cases;
    }

    public ArrayList<String> getCaseDetails(int case_id) throws ParseException {
        ArrayList<String> details = new ArrayList<String>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");

        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(GET_CASE_BY_CASE_ID);
            ps.setInt(1, case_id);
            rs = ps.executeQuery();

            while (rs.next()) {
                String nric = rs.getString("nric");
                String email = rs.getString("email");
                String cName = rs.getString("name");
                String contact_no = String.valueOf(rs.getInt("contact_no"));
                String description = rs.getString("description");
                String additional_info = rs.getString("additional_info");
                String addOnDate = rs.getString("add_on_date");
                String difficulty = rs.getString("difficulty");
                String issue = rs.getString("issue");
                String responseMsg = rs.getString("response");
                if (additional_info == null) {
                    additional_info = "N/A";
                }
                if (addOnDate == null) {
                    addOnDate = "";
                } else {
                    java.util.Date d = sdf1.parse(addOnDate);
                    addOnDate = "(added on " + sdf2.format(d) + ")";
                }
                String employeeInCharge = String.valueOf(rs.getInt("employee_id"));
                details.add(String.valueOf(case_id));
                details.add(nric);
                details.add(email);
                details.add(cName);
                details.add(contact_no);
                details.add(description);
                details.add(additional_info);
                details.add(difficulty);
                details.add(issue);
                details.add(responseMsg);                
                details.add(employeeInCharge);
                details.add(addOnDate);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return details;
    }

    public int updateCaseResponse(String response, String datetime, int employeeID, int caseID) {
        Connection con = null;
        PreparedStatement ps = null;
        int rowUpdate = 0;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(UPDATE_CASE_RESPONSE);
            ps.setString(1, response);
            ps.setString(2, datetime);
            ps.setString(3, datetime);
            ps.setInt(4, employeeID);
            ps.setInt(5, caseID);
            rowUpdate = ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rowUpdate;
    }

    public int saveCaseResponse(String response, String datetime, int employeeID, int caseID) {
        Connection con = null;
        PreparedStatement ps = null;
        int rowUpdate = 0;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(SAVE_CASE_RESPONSE);
            ps.setString(1, response);
            ps.setString(2, datetime);
            ps.setInt(3, employeeID);
            ps.setInt(4, caseID);
            rowUpdate = ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rowUpdate;
    }

    public int routeCaseToRespectiveInCharge(int caseID, int employeeID, String datetime) {
        Connection con = null;
        PreparedStatement ps = null;
        int rowUpdate = 0;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(INSERT_TO_CASEHANDLING);
            ps.setInt(1, caseID);
            ps.setInt(2, employeeID);
            ps.setString(3, datetime);
            rowUpdate = ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rowUpdate;
    }

    public int updateCaseStatus(String status, int caseID) {
        Connection con = null;
        PreparedStatement ps = null;
        int rowUpdate = 0;
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(UPDATE_CASE_STATUS);
            ps.setString(1, status);
            ps.setInt(2, caseID);
            rowUpdate = ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rowUpdate;
    }

    public LinkedHashMap<Integer, ArrayList<String>> getCaseResponses(int caseID) {
        LinkedHashMap<Integer, ArrayList<String>> map = new LinkedHashMap<Integer, ArrayList<String>>();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(GET_CASE_RESPONSES);
            ps.setInt(1, caseID);
            rs = ps.executeQuery();

            while (rs.next()) {
                ArrayList<String> responses = new ArrayList<String>();
                String responseDate = df.format(rs.getDate("response_date"));
                String response = rs.getString("response");
                String name = rs.getString("name") + ", " + rs.getString("position");
                int employeeID = rs.getInt("employee_id");
                responses.add(name);
                responses.add(responseDate);
                responses.add(response);
                if (!map.containsKey(employeeID)) {
                    map.put(employeeID, responses);
                } else {
                    map.get(employeeID).add(responseDate);
                    map.get(employeeID).add(response);
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    public String calculateExpectedResponseDate(int daysToAdd, String dateStr) throws ParseException {
        //Assumption: received date is date recorded when the case is created in CCMS, not when they received the email.
        String expectedDate = "";

        //Set the recorded date to calendar
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        Calendar cal = Calendar.getInstance();
        java.util.Date date = sdf1.parse(dateStr);
        dateStr = sdf2.format(date);
        cal.setTime(sdf2.parse(dateStr));

        // iterate over the dates from now and check if each day is a business day
        int businessDayCounter = 0;
        while (businessDayCounter < daysToAdd) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            //If the dayOfWeek hits Saturday, Sunday or Holiday, it will not add to businessDayCounter but it add one more day on the calendar
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY && !holidays.containsKey(cal.get(Calendar.DAY_OF_YEAR))) {
                businessDayCounter++;
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

//        sdf2 = new SimpleDateFormat("dd/MM/yyyy");
        sdf2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        expectedDate = sdf2.format(cal.getTime());
        return expectedDate;
    }
}
