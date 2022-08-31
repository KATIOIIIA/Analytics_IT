package analytics_it;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author katen'ka
 */
public class DBConnection {
private Connection myConnection;

    /** Creates a new instance of MyDBConnection */
    public DBConnection() {

    }

    public void init(){

       try{
        DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
            try {
    Class.forName("org.apache.derby.jdbc.ClientDriver");
    } catch (java.lang.ClassNotFoundException e) {
      e.printStackTrace();
    }

       
        myConnection=DriverManager.getConnection(
                "jdbc:derby://localhost:1527/DBAnalytics_IT","root", "root"
                );
        }
        catch(Exception e){
            System.out.println("Failed to get connection");
            e.printStackTrace();
        }
    }


    public Connection getMyConnection(){
        return myConnection;
    }


    public void close(ResultSet rs){

        if(rs !=null){
            try{
               rs.close();
            }
            catch(Exception e){}

        }
    }

     public void close(java.sql.Statement stmt){

        if(stmt !=null){
            try{
               stmt.close();
            }
            catch(Exception e){}

        }
    }

  public void destroy(){

    if(myConnection !=null){

         try{
               myConnection.close();
            }
            catch(Exception e){}


    }
  }
}
