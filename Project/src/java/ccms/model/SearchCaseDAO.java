/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ccms.model;

import java.sql.*;
import java.util.*;

public class SearchCaseDAO {
    private static final String GET_ALL_CASE = "select t1.case_id, t1.reported_date, t2.name, t2.nric, t2.contact_no, t2.email, t1.type, t4.status, t1.description, t4.difficulty, t4.issue, t4.addtional_info, t4.closing_remark from \n" +
"ccms_db.case as t1 LEFT JOIN ccms_db.person as t2 ON t1.person_nric=t2.nric,\n" +
"ccms_db.case as t3 LEFT JOIN ccms_db.complaint_case as t4 ON t3.case_id=t4.complaint_case_id;";
    private static final String SEARCH_CASE = "select t1.case_id, t1.reported_date, t2.name, t2.nric, t2.contact_no, t2.email, t1.type, t4.status, t1.description, t4.difficulty, t4.issue, t4.addtional_info, t4.closing_remark from \n" +
"ccms_db.case as t1 LEFT JOIN ccms_db.person as t2 ON t1.person_nric=t2.nric,\n" +
"ccms_db.case as t3 LEFT JOIN ccms_db.complaint_case as t4 ON t3.case_id=t4.complaint_case_id WHERE \n"+
"t1.case_id=? AND t2.nric=?";
    private static final String SHOW_CASE = "select t1.case_id, t1.reported_date, t2.name, t2.nric, t2.contact_no, t2.email, t1.type, t4.status, t1.description, t4.difficulty, t4.issue, t4.addtional_info, t4.closing_remark from \n" +
"ccms_db.case as t1 LEFT JOIN ccms_db.person as t2 ON t1.person_nric=t2.nric,\n" +
"ccms_db.case as t3 LEFT JOIN ccms_db.complaint_case as t4 ON t3.case_id=t4.complaint_case_id WHERE \n"+
"t1.case_id=? AND t2.nric=?";
    private static final String ADD_INFO = "update ccms_db.complaint_case SET addtional_info=? WHERE \n"+
            "complaint_case_id=?";
    private static final String ARCHIVE_CASE = "update ccms_db.complaint_case SET closing_remark=?, status='Closed' WHERE \n"+
            "complaint_case_id=?";
    
    public SearchCaseDAO(){
    }
    
    public ArrayList<SearchCase> getAllCase() {
        ArrayList<SearchCase> caseList = new ArrayList<SearchCase>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(GET_ALL_CASE);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                SearchCase case1 = new SearchCase(
                        rs.getInt("case_id"),
                        rs.getString("reported_date"),
                        rs.getString("name"),
                        rs.getString("nric"),
                        rs.getInt("contact_no"),
                        rs.getString("email"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getString("description"),
                        rs.getString("difficulty"),
                        rs.getString("issue"),
                        rs.getString("addtional_info"),
                        rs.getString("closing_remark"));
                caseList.add(case1);
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
        return caseList;
    }
    
    public ArrayList<SearchCase> searchCase(int caseID, String nric) {
        ArrayList<SearchCase> caseList = new ArrayList<SearchCase>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(SEARCH_CASE);
            ps.setInt(1, caseID);
            ps.setString(2, nric);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                SearchCase case1 = new SearchCase(
                        rs.getInt("case_id"),
                        rs.getString("reported_date"),
                        rs.getString("name"),
                        rs.getString("nric"),
                        rs.getInt("contact_no"),
                        rs.getString("email"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getString("description"),
                        rs.getString("difficulty"),
                        rs.getString("issue"),
                        rs.getString("addtional_info"),
                        rs.getString("closing_remark"));
                caseList.add(case1);
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
        return caseList;
    }
    
    public ArrayList<SearchCase> showCase(int caseID, String nric) {
        ArrayList<SearchCase> caseList = new ArrayList<SearchCase>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = ConnectionManager.getConnection();
            ps = con.prepareStatement(SHOW_CASE);
            ps.setInt(1, caseID);
            ps.setString(2, nric);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                SearchCase case1 = new SearchCase(
                        rs.getInt("case_id"),
                        rs.getString("reported_date"),
                        rs.getString("name"),
                        rs.getString("nric"),
                        rs.getInt("contact_no"),
                        rs.getString("email"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getString("description"),
                        rs.getString("difficulty"),
                        rs.getString("issue"),
                        rs.getString("addtional_info"),
                        rs.getString("closing_remark"));
                caseList.add(case1);
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
        return caseList;
    }
    
    public boolean addInfo(String addInfo, int caseID) {
        
        boolean success = false;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;


        try {

            conn = ConnectionManager.getConnection();
            
            pstmt = conn.prepareStatement(ADD_INFO);
            pstmt.setString(1, addInfo);
            pstmt.setInt(2, caseID);

            if (pstmt.executeUpdate() > 0) {
                success = true;
            }

        } catch (SQLException e) {
            success = false;

        }

        return success;
    }
    
     public boolean archiveCase(String closingRemark, int caseID) {
        
        boolean success = false;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;


        try {

            conn = ConnectionManager.getConnection();
            
            pstmt = conn.prepareStatement(ARCHIVE_CASE);
            pstmt.setString(1, closingRemark);
            pstmt.setInt(2, caseID);

            if (pstmt.executeUpdate() > 0) {
                success = true;
            }

        } catch (SQLException e) {
            success = false;

        }

        return success;
    }
}
