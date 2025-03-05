import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbTest {
	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		Connection con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:4000/test?connectTimeout=3000&socketTimeout=10000&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&useOldAliasMetadataBehavior=true","root","123456");
		ResultSet rs = con.createStatement().executeQuery("select @@version");
		if(rs.next())
		System.out.println(rs.getString(1));
	}
}
