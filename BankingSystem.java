import java.sql.*;
import java.util.Scanner;

public class BankingSystem {

    // jdbc connection

    static final String URL = "jdbc:mysql:// localhost:3306/bankdb";
    static final String USER = "root";
    static final String PASS = "Ranjith03@#";

    public static void main(String[] args) throws SQLException {
        Scanner scanner = new Scanner(System.in);
       try(Connection conn = DriverManager.getConnection(URL,USER,PASS)){
           while(true){
               System.out.println("********************");
               System.out.println("1.Create Account");
               System.out.println("2.Deposit");
               System.out.println("3.Withdraw");
               System.out.println("4.Balance Enquiry");
               System.out.println("5.Money Transfer");
               System.out.println("6.View Transaction");
               System.out.println("7.Exit");
               System.out.println("********************");
               System.out.print("Enter choice:");

               int choice = scanner.nextInt();


               switch (choice){

                   case 1: CreateAccount(conn, scanner); break;
                   case 2: deposit(conn, scanner); break;
                   case 3: withdraw (conn,scanner); break;
                   case 4: balanceEnquiry (conn, scanner); break;
                   case 5: TransferMoney(conn,scanner); break;
                   case 6: ViewTransactions(conn,scanner); break;

                   case 7: System.exit(0);

                   default:
                       System.out.println("invalid choice");
               }
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    // 1.Create Account
    static void CreateAccount(Connection conn, Scanner scanner) throws SQLException
    {
        System.out.print("Enter name:");
        String name = scanner.next();

        String sql = "INSERT into accounts (name,balance) values (?,0)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString (1,name);
        ps.executeUpdate();
        System.out.println("Account Created Successfully for " +name );
    }

    //2.Deposit

    static void deposit(Connection conn ,Scanner scanner)throws SQLException
    {
        System.out.print("Enter Account ID:");
        int id=scanner.nextInt();
        System.out.print("Enter your amount to deposit: ");
        double amount = scanner.nextDouble();

        String sql ="UPDATE accounts SET balance = balance +? WHERE account_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDouble(1,amount);
        ps.setInt(2,id);

        int rows = ps.executeUpdate();


        if (rows > 0){
            System.out.println("Account.No:" +id +" RS." +amount +" Credited Successfull");

            // Insert transaction table
            String tsql = "INSERT INTO transactions(account_id,type,amount) VALUES(?,'deposit',?)";
            PreparedStatement tps = conn.prepareStatement(tsql);
            tps.setInt(1,id);
            tps.setDouble(2,amount);
            int inserted =tps.executeUpdate();
        }
        else{
            System.out.println("Account Not Found!");
        }
    }

    //3.Withdraw

    static void withdraw(Connection conn, Scanner scanner) throws SQLException
    {
        System.out.print("Enter Account ID:");
        int id = scanner.nextInt();
        System.out.print("Enter amount to withdraw:");
        double amount = scanner.nextDouble();

        // check balance first

        String checksql ="SELECT balance FROM accounts WHERE account_id=?";
        PreparedStatement checkps = conn.prepareStatement(checksql);
        checkps.setInt(1,id);
         ResultSet rs = checkps.executeQuery();

         if(rs.next()) {
             double balance = rs.getDouble("balance");
             if (balance >= amount) {
                 String sql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ps.setDouble(1, amount);
                 ps.setInt(2, id);
                 ps.executeUpdate();
                 System.out.println("Withdraw Successfull");

                 //INSERT withdraw into transaction table

                 String tsql = "INSERT INTO transactions(account_id,type,amount) VALUES(?,'withdraw',?)";
                 PreparedStatement tps = conn.prepareStatement(tsql);
                 tps.setInt(1, id);
                 tps.setDouble(2, amount);
                 int inserted = tps.executeUpdate();
             } else {
                 System.out.println("Insufficient Balance");
             }

         }
         else {
                 System.out.println("Account not Found!");

         }
    }
    //4. Balance Enquiry

    static void balanceEnquiry(Connection conn,Scanner scanner) throws SQLException
    {
        System.out.print("Enter Account ID: ");
        int id = scanner.nextInt();
        String sql ="SELECT name,balance FROM accounts WHERE account_id = ? ";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,id);
        ResultSet rs = ps.executeQuery();

        if(rs.next()){
            System.out.println("Name:" + rs.getString("name"));
            System.out.println("Balance:" +rs.getString("Balance"));
        }
        else {
            System.out.println("Account not found!");
        }

    }

    //  Insert into View Transaction

    static void ViewTransactions(Connection conn, Scanner scanner) throws SQLException{
        System.out.print("Enter Account ID:");
        int id = scanner.nextInt();

        String sql ="SELECT transaction_id,type,amount,transaction_date FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,id);
        ResultSet rs=ps.executeQuery();
        System.out.println("-------Transaction History---------");
        while(rs.next()){
            System.out.println(
                    rs.getInt("transaction_id") +"|" +
                    rs.getString("type") +"|" +
                    rs.getDouble("amount") +"|" +
                    rs.getTimestamp("transaction_date")
            );
        }
    }

    // Insert Transfer Money 1 account to other account
    static void TransferMoney(Connection conn,Scanner scanner) {
        System.out.print("Enter Sender Account Id:");
        int fromId = scanner.nextInt();
        System.out.print("Enter Receiver Account Id: ");
        int toId = scanner.nextInt();
        System.out.print("Enter Amount to transfer:");
        double amount = scanner.nextDouble();

        if (amount <= 0) {
            System.out.println("Amount must be greater than 0.");
            return;
        }
        if (fromId == toId) {
            System.out.println("Sender & Receiver can't be the same.");
            return;
        }
        try {
            conn.setAutoCommit(false);

            // Check Sender Balance
            String sql = "SELECT balance FROM accounts WHERE account_id = ?";
            PreparedStatement checkps = conn.prepareStatement(sql);
            checkps.setInt(1, fromId);
            ResultSet rs = checkps.executeQuery();
            if (!rs.next()) {
                System.out.println("Sender account not found");
                conn.rollback();
                return;
            }

            double senderBalance = rs.getDouble("balance");
            if (senderBalance < amount) {
                System.out.println("Insufficient balance in sender Account.");
                conn.rollback();
                return;
            }

            // Check Reciever Exists

            String recevsql = "SELECT account_id FROM accounts WHERE account_id = ?";
            PreparedStatement recevps = conn.prepareStatement(recevsql);
            recevps.setInt(1, toId);
            ResultSet rs2 = recevps.executeQuery();
            if (!rs2.next()) {
                System.out.println("Receiver account not found!");
                conn.rollback();
                return;
            }

            // Deduct from sender

            String dedsql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            PreparedStatement dedps = conn.prepareStatement(dedsql);
            dedps.setDouble(1, amount);
            dedps.setInt(2, fromId);
            dedps.executeUpdate();

            //Credit to Receiver

            String cresql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            PreparedStatement creps = conn.prepareStatement(cresql);
            creps.setDouble(1, amount);
            creps.setInt(2, toId);
            creps.executeUpdate();

            // Insert transaction logs

            String tsql = "INSERT INTO transactions (account_id,type,amount) VALUES (?,?,?)";
            PreparedStatement tps = conn.prepareStatement(tsql);

            //Sender withdraw

            tps.setInt(1, fromId);
            tps.setString(2, "withdraw");
            tps.setDouble(3, amount);
            tps.executeUpdate();

            // Receive deposit

            tps.setInt(1, toId);
            tps.setString(2, "deposit");
            tps.setDouble(3, amount);
            tps.executeUpdate();

            conn.commit();
            System.out.println("Transfer Successfull.RS." + amount + " from account " + fromId + " to account " + toId);
        } catch (SQLException ex) {
            try {
                conn.rollback();
                System.out.println("Transfer failed.Rolled back");
            } catch (Exception rbEx) {
                rbEx.printStackTrace();
                ex.printStackTrace();
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex2) {
                    ex2.printStackTrace();
                }
            }
        }

    }
}


