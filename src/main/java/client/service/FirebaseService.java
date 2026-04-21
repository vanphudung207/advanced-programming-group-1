package client.service;
import java.io.*;
//đọc dữ liệu từ server
import java.net.*;
//tạo url và kết nối HTTP
public class FirebaseService {
    private static final String BASE_URL= "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app/";
    //url firebase

    //lấy thông tin của sản phầm đấu giá theo id
    public static String getAuction(String id) throws Exception{
        //tạo url tới node cần lấy (auctions/id.json)
        URL url =new URL(BASE_URL +"auctions/" + id+ ".json");
        //mở cổng http đến url trên
        HttpURLConnection conn=(HttpURLConnection) url.openConnection();

        //thiết lập get để lấy dữ liệu
        conn.setRequestMethod("GET");

        //tạo luồng đọc dữ liệu từ server trả về
        BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line;
        StringBuilder result=new StringBuilder();

        while((line=in.readLine()) !=null){
            result.append(line);
        }

        in.close(); //đóng luồng

        //trả về Json dạng String
        return result.toString();
    }
}
