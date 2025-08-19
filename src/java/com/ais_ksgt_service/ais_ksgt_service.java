package com.ais_ksgt_service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.jws.WebService;
import javax.jws.WebParam;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.commons.codec.binary.Base64;

@WebService(serviceName = "ais_ksgt_service")
public class ais_ksgt_service {

    /**
     * @t Function
     * @description Логирование ошибок
     */
    private void logError(String functionName, String device_id, String description, String errorText) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());

        System.out.println("LogError: " + formatter.format(date) + " |  Function: " + functionName + " | called by [" + device_id + "] | Description or Result: " + description + " | Error text: " + errorText);
    }

    /**
     * @t Function
     * @description Логирование вызовов и результатов методов
     */
    private void log(String functionName, String device_id, String description) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());

        System.out.println("Log: " + formatter.format(date) + " | Called by [" + device_id + "] | Function: " + functionName + " | Description or Result: " + description);
    }
    
    /**
     * @t Function
     * @description Отправка результата обхода объектов в базу данных
     * @param objects Класс с объектами (класс SendObjects в виде JSON строки) для сохранения в базе данных 
     * @param user_id Идентификатор пользователя
     * @param device_id Уникальный идентификатор устройства (указывается при логировании)
     * @return Результат сохранения в базу данных (класс dbSendObjectsResponse в виде JSON строки)
     */
    public String sendObjects(                        
            @WebParam(name = "objects") String objects,
            @WebParam(name = "user_id") String user_id,
            @WebParam(name = "device_id") String device_id) {

        String CURRENT_FUNCTION = new Throwable().getStackTrace()[0].getMethodName();
        log(CURRENT_FUNCTION, device_id, "Вызван метод");

        String JsonOutput = ""; // Результат отправки объектов в БД
       
        DatabaseSendObjectsResponse dbSendObjectsResponse = new DatabaseSendObjectsResponse();
        
        Gson gson_ = new GsonBuilder().create();
        SendObjects so = gson_.fromJson(objects, SendObjects.class);
      
        int check_counter = 0;
        int delict_counter = 0;
        
        List<PObject> objects_tmp = so.objects;
        List<String> plans_id_tmp = so.plans_id;
       
        DataSource ds;
        try {
            // Инициализация контекста и источника данных
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/oracle11g");

            // SQL запросы к БД
            // Сбросить признак обхода SIGN_REVIEW для всех объектов выгружаемых планов
            String query0 = "UPDATE T_KT_SUB_CONTROL SET SIGN_REVIEW='0' WHERE PLAN_ID=(?)";
            
            // Получить идентификаторы всех подконтрольных объектов по выгружаемым планам
            String query0_1 = "SELECT SUB_CONTROL_ID FROM T_KT_SUB_CONTROL WHERE PLAN_ID=(?)";
            
            // Получить идентификаторы всех нарушений по выгружаемым планам
            String query0_2 = "SELECT DELICT_ID FROM T_KT_DELICT WHERE SUB_CONTROL_ID=(?)";
            
            
            // Удаление старых нарушений по объекту
            String query2_0 = "DELETE FROM T_KT_DELICT WHERE SUB_CONTROL_ID=(?)";

            // Получаем идентификатор файла по идентификатору подконтрольного объекта
            String query2_1 = "SELECT UUID FROM T_KT_FILE_OBJECT WHERE SUB_OBJECT_ID=(?)";
            
            // Удаляем ссылку на таблицу с фото
            String query2_2 = "DELETE FROM T_KT_FILE_OBJECT WHERE SUB_OBJECT_ID=(?)";
            
            // Удаляем фото из таблицы
            String query2_3 = "DELETE FROM E3_BLOB WHERE FILE_UUID=(?)";
            
            // Получаем идентификатор файла по идентификатору нарушения
            String query2_4 = "SELECT UUID FROM T_KT_FILE_DELICT WHERE DELICT_ID=(?)";
            
            // Удаляем ссылку на таблицу с фото
            String query2_5 = "DELETE FROM T_KT_FILE_DELICT WHERE DELICT_ID=(?)";
            
            // Удаляем фото из таблицы
            String query2_6 = "DELETE FROM E3_BLOB WHERE FILE_UUID=(?)";
            

            // Устанавливаем признак обхода
            String query3 = "UPDATE T_KT_SUB_CONTROL SET SIGN_REVIEW='1' WHERE SUB_CONTROL_ID=(?)";
                               
            // Добавление фото в таблицу с файлами
            String query4 = "INSERT INTO E3_BLOB (FILE_UUID, FILE_NAME, CONTENT_TYPE, FILE_DATA, CREATE_DATE, IS_TEMPORARY, IS_DELETED)"
                    + "VALUES((?), (?), (?), (?), sysdate, '0', '0')";
            
            // Добавление ссылки на фото нарушения
            String query5 = "INSERT INTO T_KT_FILE_DELICT (DELICT_ID, UUID, DATE_INSERT) VALUES(T_KT_DELICT_seq.currval, (?), sysdate)";

            // Добавление ссылки на фото объекта (панорама и номер дома)
            String query6 = "INSERT INTO T_KT_FILE_OBJECT (SUB_OBJECT_ID, UUID, DATE_INSERT) VALUES((?), (?), sysdate)";
            
            // Добавление новой записи с нарушением в таблицу T_KT_DELICT
            String query7 = "INSERT INTO T_KT_DELICT "
                                    + "(ADDRESS_ID, ADDRESS_PLACE, DEFECTION_ID, DELICT_FIX, DATE_INSERT, OBJECT_TYPE, SUB_CONTROL_ID, TIME_WITH, TIME_FOR, USER_ID, KEY_DEPARTMENT, CAD_NUMBER, DELICT_DATE)"
                                    + "VALUES((?), (?), (?), (?), sysdate, '49201', (?), (?), (?), (?), "
                                    + "(select KEY_DEPARTMENT from t_kt_sub_control where SUB_CONTROL_ID=(?)), "
                                    + "(select CAD_NUMBER from t_kt_sub_control where SUB_CONTROL_ID=(?)),"
                                    + "trunc(sysdate))";
            
            // Подключение к БД, проверка пароля и получение данных пользователя
            try (Connection con = ds.getConnection();
                    
                    PreparedStatement ps0 =  con.prepareStatement(query0);
                    PreparedStatement ps0_1 =  con.prepareStatement(query0_1);
                    PreparedStatement ps0_2 =  con.prepareStatement(query0_2);
                    
                    PreparedStatement ps2_0 = con.prepareStatement(query2_0);
                    PreparedStatement ps2_1 = con.prepareStatement(query2_1);
                    PreparedStatement ps2_2 = con.prepareStatement(query2_2);
                    PreparedStatement ps2_3 = con.prepareStatement(query2_3);
                    PreparedStatement ps2_4 = con.prepareStatement(query2_4);
                    PreparedStatement ps2_5 = con.prepareStatement(query2_5);
                    PreparedStatement ps2_6 = con.prepareStatement(query2_6);
                    
                    PreparedStatement ps3 = con.prepareStatement(query3);
                    PreparedStatement ps4 = con.prepareStatement(query4);
                    PreparedStatement ps5 = con.prepareStatement(query5);
                    PreparedStatement ps6 = con.prepareStatement(query6);
                    PreparedStatement ps7 = con.prepareStatement(query7)) {
                
                /**
                 * Очистка старого обхода
                 */
                // Query0 | Сбросить признак обхода SIGN_REVIEW для всех объектов выгружаемых планов
                // UPDATE T_KT_SUB_CONTROL SET SIGN_REVIEW='0' WHERE PLAN_ID=(?)
                for (int i = 0; i < plans_id_tmp.size(); i++) {
                    ps0.setString(1, plans_id_tmp.get(i));
                    ps0.executeUpdate();
                }
                
                // Query0_1 | Получить идентификаторы всех подконтрольных объектов по выгружаемым планам
                // SELECT SUB_CONTROL_ID FROM T_KT_SUB_CONTROL WHERE PLAN_ID=(?)
                
                List<String> id_objects_for_clear = new ArrayList();
                for (int i = 0; i < plans_id_tmp.size(); i++) {
                    ps0_1.setString(1, plans_id_tmp.get(i));
                    ResultSet rs0_1 = ps0_1.executeQuery();
                    while (rs0_1.next()) {
                        id_objects_for_clear.add(rs0_1.getString("SUB_CONTROL_ID")); 
                    }
                }
                
                // Query0_2 | Получить идентификаторы всех нарушений по выгружаемым планам
                // SELECT DELICT_ID FROM T_KT_DELICT WHERE SUB_CONTROL_ID=(?)
                List<String> id_delicts_for_clear = new ArrayList();
                for (int i = 0; i < id_objects_for_clear.size(); i++) {
                    ps0_2.setString(1, id_objects_for_clear.get(i));
                    ResultSet rs0_2 = ps0_2.executeQuery();
                    while (rs0_2.next()) {
                        id_delicts_for_clear.add(rs0_2.getString("DELICT_ID"));
                    }
                }
                
                for (int i = 0; i < id_objects_for_clear.size(); i++) {
                    
                    // Query2_0 | Удаление старых правонарушений по объекту
                    // DELETE FROM T_KT_DELICT WHERE SUB_CONTROL_ID=(?)
                    ps2_0.setString(1, id_objects_for_clear.get(i));
                    ps2_0.executeUpdate();

                    // Query2_1 | Получаем идентификатор фото по идентификатору подконтрольного объекта
                    // SELECT UUID FROM T_KT_FILE_OBJECT WHERE SUB_OBJECT_ID=(?)
                    ps2_1.setString(1, id_objects_for_clear.get(i));
                    ResultSet rs2_1 = ps2_1.executeQuery();
                    List<String> uuid_sub_objects = new ArrayList();
                    while (rs2_1.next()) {
                        uuid_sub_objects.add(rs2_1.getString("UUID"));
                    }

                    // Query2_2 | Удаляем ссылку на таблицу с фото
                    // DELETE FROM T_KT_FILE_OBJECT WHERE SUB_OBJECT_ID=(?)
                    ps2_2.setString(1, id_objects_for_clear.get(i));
                    ps2_2.executeUpdate();

                    // Query2_3 | Удаляем фото из таблицы
                    // DELETE FROM E3_BLOB WHERE FILE_UUID=(?)
                    for (int z = 0; z < uuid_sub_objects.size(); z++) {
                        ps2_3.setString(1, uuid_sub_objects.get(z));
                        ps2_3.executeUpdate();
                    }
                }
                
                for (int i = 0; i < id_delicts_for_clear.size(); i++) {
                    
                    // Query2_4 | Получаем идентификатор файла по идентификатору нарушения
                    // SELECT UUID FROM T_KT_FILE_DELICT WHERE DELICT_ID=(?)
                    ps2_4.setString(1, id_delicts_for_clear.get(i));
                    ResultSet rs2_4 = ps2_4.executeQuery();
                    List<String> uuid_delicts = new ArrayList();
                    while (rs2_4.next()) {
                        uuid_delicts.add(rs2_4.getString("UUID"));
                    }
                    
                    // Query2_5 | Удаляем ссылку на таблицу с фото
                    // DELETE FROM T_KT_FILE_DELICT WHERE DELICT_ID=(?
                    ps2_5.setString(1, id_delicts_for_clear.get(i));
                    ps2_5.executeUpdate();

                    for (int z = 0; z < uuid_delicts.size(); z++) {

                        // Query2_6 | Удаляем фото из таблицы
                        // DELETE FROM E3_BLOB WHERE FILE_UUID=(?)
                        ps2_6.setString(1, uuid_delicts.get(z));
                        ps2_6.executeUpdate();
                    }
                }
                
                /**
                 * Выгрузка нового обхода
                 */
                for (int i = 0; i < objects_tmp.size(); i++) { // Обход объектов

                    // Query3 | Устанавливаем признак обхода
                    // UPDATE T_KT_SUB_CONTROL SET SIGN_REVIEW='1' WHERE SUB_CONTROL_ID=(?)
                    ps3.setString(1, objects_tmp.get(i).sub_control_id);
                    ps3.executeUpdate();

                    if (objects_tmp.get(i).delicts != null) {
                        
                        // Query4 | Добавление фото панорамы в таблицу с файлами
                        // INSERT INTO E3_BLOB (FILE_UUID, FILE_NAME, CONTENT_TYPE, FILE_DATA, CREATE_DATE, IS_TEMPORARY, IS_DELETED) VALUES((?), (?), (?), (?), sysdate, '0', '0')
                        String uuid1 = UUID.randomUUID().toString();
                        String file_name1 = "panorama" + user_id + "_" + device_id + "_" + objects_tmp.get(i).sub_control_id + ".jpg";
                        if (objects_tmp.get(i).photo_panorama_base64 != null) {
                            
                            ps4.setString(1, uuid1);
                            ps4.setString(2, file_name1);
                            byte[] bytePhotoPanorama = Base64.decodeBase64(objects_tmp.get(i).photo_panorama_base64);
                            InputStream in = new ByteArrayInputStream(bytePhotoPanorama);
                            String mimeType = URLConnection.guessContentTypeFromStream(in);
                            ps4.setString(3, mimeType);
                            ps4.setBinaryStream(4, in, bytePhotoPanorama.length);
                            ps4.executeUpdate();
                            
                            // Query6 | Добавление ссылки фото панорамы в таблицу T_KT_FILE_DELICT
                            // INSERT INTO T_KT_FILE_OBJECT (SUB_OBJECT_ID, UUID, DATE_INSERT) VALUES((?), (?), sysdate)
                            ps6.setString(1, objects_tmp.get(i).sub_control_id);
                            ps6.setString(2, uuid1);
                            ps6.executeUpdate();
                        }
                               
                        
                        // Query4 | Добавление фото номера дома в таблицу с файлами
                        String uuid2 = UUID.randomUUID().toString();
                        String file_name2 = "house_number" + user_id + "_" + device_id + "_" + objects_tmp.get(i).sub_control_id + ".jpg";
                        if (objects_tmp.get(i).photo_house_number_base64 != null) {
                            ps4.setString(1, uuid2);
                            ps4.setString(2, file_name2);
                            byte[] bytePhotoHouserNumber = Base64.decodeBase64(objects_tmp.get(i).photo_house_number_base64);
                            InputStream in = new ByteArrayInputStream(bytePhotoHouserNumber);
                            String mimeType = URLConnection.guessContentTypeFromStream(in);
                            ps4.setString(3, mimeType);
                            ps4.setBinaryStream(4, in, bytePhotoHouserNumber.length);
                            ps4.executeUpdate();
                            
                            // Query6 | Добавление ссылки на фото номера дома в таблицу T_KT_FILE_DELICT
                            ps6.setString(1, objects_tmp.get(i).sub_control_id);
                            ps6.setString(2, uuid2);
                            ps6.executeUpdate();
                        }
                        
                        for (int j = 0; j < objects_tmp.get(i).delicts.size(); j++) { // Обход нарушений
                            
                            // Query6 | Добавляем запись с нарушением в таблицу T_KT_DELICT
                            ps7.setString(1, objects_tmp.get(i).address_id);
                            ps7.setString(2, objects_tmp.get(i).address_place);
                            ps7.setString(3, objects_tmp.get(i).delicts.get(j).defection_id);
                            ps7.setString(4, objects_tmp.get(i).delicts.get(j).defection_name);
                            ps7.setString(5, objects_tmp.get(i).sub_control_id);
                            ps7.setString(6, objects_tmp.get(i).delicts.get(j).time);
                            ps7.setString(7, objects_tmp.get(i).delicts.get(j).time);
                            ps7.setString(8, user_id);
                            ps7.setString(9, objects_tmp.get(i).sub_control_id);
                            ps7.setString(10, objects_tmp.get(i).sub_control_id);
                            ps7.executeUpdate();
                            
                            // Query4 | Добавление фото нарушения в таблицу с файлами в таблицу E3_BLOB
                            String uuid3 = UUID.randomUUID().toString();
                            String file_name3 = "delict" + user_id + "_" + device_id + "_" + objects_tmp.get(i).sub_control_id + "_" + objects_tmp.get(i).delicts.get(j).defection_id + ".jpg";
                            if (objects_tmp.get(i).delicts.get(j).photo_base64 != null) {
                                ps4.setString(1, uuid3);
                                ps4.setString(2, file_name3);
                                byte[] bytePhotoDelict = Base64.decodeBase64(objects_tmp.get(i).delicts.get(j).photo_base64);
                                InputStream in = new ByteArrayInputStream(bytePhotoDelict);
                                String mimeType = URLConnection.guessContentTypeFromStream(in);
                                ps4.setString(3, mimeType);
                                ps4.setBinaryStream(4, in, bytePhotoDelict.length);
                                ps4.executeUpdate();
                                
                                // Query5 | Добавление ссылки на фото нарушения в таблицу T_KT_FILE_DELICT
                                // INSERT INTO T_KT_FILE_DELICT (DELICT_ID, UUID, DATE_INSERT) VALUES(T_KT_DELICT_seq.currval, (?), sysdate)
                                ps5.setString(1, uuid3);
                                ps5.executeUpdate();
                            }
                            
                            delict_counter++;
                        }
                    }
                    check_counter++;
                }
                
                // Отправка результата
                dbSendObjectsResponse.success = true;
                dbSendObjectsResponse.resultMessage = "Результат обхода успешно выгружен\nПроверок: " + check_counter + "\nНарушений: " + delict_counter;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonOutput = gson.toJson(dbSendObjectsResponse);
                
            } catch (SQLException e) {
                logError(CURRENT_FUNCTION, device_id, "Не удалось установить соединение с БД", e.toString());
                dbSendObjectsResponse.success = false;
                dbSendObjectsResponse.errorMessage = "Не удалось установить соединение с БД";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbSendObjectsResponse);
            } catch (Exception e) {
                logError(CURRENT_FUNCTION, device_id, "Ошибка", e.toString());
                dbSendObjectsResponse.success = false;
                dbSendObjectsResponse.errorMessage = "Неизвестная ошибка";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbSendObjectsResponse);
            }
        } catch (Exception e) {
            logError(CURRENT_FUNCTION, device_id, "Не удалось открыть источник данных из Context.xml", e.toString());
            dbSendObjectsResponse.success = false;
            dbSendObjectsResponse.errorMessage = "Не удалось открыть источник данных из Context.xml";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(dbSendObjectsResponse);
        }

        log(CURRENT_FUNCTION, device_id, "Результат работы функции: " + JsonOutput);
        return JsonOutput;
    }

    /**
     * @t Function
     * @description Авторизация в АИС Контроль содержания городских территорий
     * @param login Логин пользователя
     * @param password Пароль пользователя
     * @param device_id Уникальный идентификатор устройства (указывается при логировании)
     * @return Результат попытки авторизации (класс DatabaseLoginResponse в виде JSON строки)
     */
    public String tryLogin(
            @WebParam(name = "login") String login,
            @WebParam(name = "password") String password,
            @WebParam(name = "device_id") String device_id) {

        String CURRENT_FUNCTION = new Throwable().getStackTrace()[0].getMethodName();
        log(CURRENT_FUNCTION, device_id, "Вызван метод");

        String JsonOutput = ""; // Результат авторизации в виде JSON строки
   
        DatabaseLoginResponse dbLoginResponse = new DatabaseLoginResponse(); // Результат авторизации

        DataSource ds;
        try {
            // Инициализация контекста и источника данных
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/oracle11g");

            // SQL запросы к БД
            // Получение password, last_name, first_name, second_name, user_id по login из таблицы USER_TABLE
            String query1 = "SELECT PASSWORD, LAST_NAME, FIRST_NAME, SECOND_NAME, USER_ID FROM USER_TABLE WHERE LOGIN=(?)";

            // Получение всех represent_id и official_id по ФИО из таблицы tv_s_official_o
            String query2 = "SELECT REPRESENT_ID, OFFICIAL_ID FROM TV_S_OFFICIAL_O WHERE LAST_NAME=(?) AND FIRST_NAME=(?) AND SECOND_NAME=(?)";

            // Получение всех represent_label по represent_id из таблицы tv_s_represent_o
            String query3 = "SELECT REPRESENT_LABEL FROM TV_S_REPRESENT_O WHERE REPRESENT_ID=(?)";

            // Подключение к БД, проверка пароля и получение данных пользователя
            try (Connection con = ds.getConnection();
                    PreparedStatement ps1 = con.prepareStatement(query1);
                    PreparedStatement ps2 = con.prepareStatement(query2);
                    PreparedStatement ps3 = con.prepareStatement(query3);) {

                // Query1 | Получение password, last_name, first_name, second_name, user_id по login из таблицы USER_TABLE
                ps1.setString(1, login);

                ResultSet rs1 = ps1.executeQuery();

                // Не найдено записей с введённым логином
                if (!rs1.isBeforeFirst()) {
                    log(CURRENT_FUNCTION, device_id, "Неверный логин или пароль");
                    dbLoginResponse.success = false;
                    dbLoginResponse.errorMessage = "Неверный логин или пароль";
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    return gson.toJson(dbLoginResponse);
                }

                String password_from_user = password;
                String password_from_database = "";
                String first_name = "";
                String second_name = "";
                String last_name = "";
                String user_id = "";

                while (rs1.next()) {

                    password_from_database = rs1.getString("PASSWORD");
                    first_name = rs1.getString("FIRST_NAME");
                    second_name = rs1.getString("SECOND_NAME");
                    last_name = rs1.getString("LAST_NAME");
                    user_id = rs1.getString("USER_ID");
                }

                // Сравнения хэша password из бд с хэшем введённого password
                if (BCrypt.checkpw(password_from_user, password_from_database)) {

                    // Query2 | Получение всех represent_id и official_id по ФИО из таблицы tv_s_official_o
                    List<Represent> represents = new ArrayList();

                    ps2.setString(1, last_name);
                    ps2.setString(2, first_name);
                    ps2.setString(3, second_name);

                    ResultSet rs2 = ps2.executeQuery();

                    // Пользователь не закреплён ни за одним подразделением
                    if (!rs2.isBeforeFirst()) {
                        log(CURRENT_FUNCTION, device_id, "Пользователь не закреплён ни за одним подразделением");
                        dbLoginResponse.success = false;
                        dbLoginResponse.errorMessage = "Пользователь не закреплён ни за одним подразделением";
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        return gson.toJson(dbLoginResponse);
                    }

                    while (rs2.next()) {
                        represents.add(new Represent(rs2.getString("REPRESENT_ID"), rs2.getString("OFFICIAL_ID")));
                    }

                    for (int i = 0; i < represents.size(); i++) {
                        
                        // Query3 | Получение всех represent_label по represent_id из таблицы tv_s_represent_o
                        ps3.setString(1, represents.get(i).getId());

                        ResultSet rs3 = ps3.executeQuery();

                        // Не найдено записи с описанием подразделения
                        if (!rs3.isBeforeFirst()) {
                            log(CURRENT_FUNCTION, device_id, "Описание одного из подразделений отсутствует");
                            dbLoginResponse.success = false;
                            dbLoginResponse.errorMessage = "Описание одного из подразделений отсутствует";
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            return gson.toJson(dbLoginResponse);
                        }

                        while (rs3.next()) {
                            represents.get(i).setLabel(rs3.getString("REPRESENT_LABEL"));
                        }
                    }

                    dbLoginResponse.success = true;
                    dbLoginResponse.user_id = user_id;
                    dbLoginResponse.first_name = first_name;
                    dbLoginResponse.second_name = second_name;
                    dbLoginResponse.last_name = last_name;
                    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                    String currentDate = df.format(Calendar.getInstance().getTime());
                    dbLoginResponse.webServiceDate = currentDate;
                    dbLoginResponse.represents = represents;
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonOutput = gson.toJson(dbLoginResponse);

                } else {
                    log(CURRENT_FUNCTION, device_id, "Неверный логин или пароль");
                    dbLoginResponse.success = false;
                    dbLoginResponse.errorMessage = "Неверный логин или пароль";
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    return gson.toJson(dbLoginResponse);
                }
            } catch (SQLException e) {
                logError(CURRENT_FUNCTION, device_id, "Не удалось установить соединение с БД", e.toString());
                dbLoginResponse.success = false;
                dbLoginResponse.errorMessage = "Не удалось установить соединение с БД";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbLoginResponse);
            } catch (Exception e) {
                logError(CURRENT_FUNCTION, device_id, "Ошибка", e.toString());
                dbLoginResponse.success = false;
                dbLoginResponse.errorMessage = "Неизвестная ошибка";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbLoginResponse);
            }
        } catch (Exception e) {
            logError(CURRENT_FUNCTION, device_id, "Не удалось открыть источник данных из Context.xml", e.toString());
            dbLoginResponse.success = false;
            dbLoginResponse.errorMessage = "Не удалось открыть источник данных из Context.xml";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(dbLoginResponse);
        }

        log(CURRENT_FUNCTION, device_id, "Результат работы функции: " + JsonOutput);
        return JsonOutput;
    }

    /**
     * @t Function
     * @description Получение видов нарушений из базы данных
     * @param device_id Уникальный идентификатор устройства (указывается при логировании)
     * @return Виды нарушений из базы данных (класс DatabaseGetDefectionsResponse в виде JSON строки)
     */
    public String getDefections(
            @WebParam(name = "device_id") String device_id) {

        String CURRENT_FUNCTION = new Throwable().getStackTrace()[0].getMethodName();
        log(CURRENT_FUNCTION, device_id, "Вызван метод");

        String JsonOutput = ""; // Результат загрузки видов нарушений в виде JSON строки

        DatabaseGetDefectionsResponse dbGetDefectionsResponse = new DatabaseGetDefectionsResponse();

        DataSource ds;
        try {
            // Инициализация контекста и источника данных
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/oracle11g");

            // SQL запрос к БД
            // Получение DEFECTION_ID, DEFECTION_LABEL, DEFECTION_NAME из таблицы S_DEFECTION
            String query = "SELECT DEFECTION_ID, DEFECTION_LABEL, DEFECTION_NAME FROM S_DEFECTION";

            // Подключение к БД и загрузка видов нарушений
            try (Connection con = ds.getConnection();
                    PreparedStatement ps = con.prepareStatement(query);) {

                ResultSet rs = ps.executeQuery();

                // Не найдено записей
                if (!rs.isBeforeFirst()) {
                    log(CURRENT_FUNCTION, device_id, "Не найдено видов нарушений в БД");
                    dbGetDefectionsResponse.success = false;
                    dbGetDefectionsResponse.errorMessage = "Не найдено видов нарушений в БД";
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    return gson.toJson(dbGetDefectionsResponse);
                }

                // Query | Получение DEFECTION_ID, DEFECTION_LABEL, DEFECTION_NAME из таблицы S_DEFECTION
                List<Defection> defections = new ArrayList();

                while (rs.next()) {
                    defections.add(new Defection(rs.getString("DEFECTION_ID"), rs.getString("DEFECTION_LABEL"), rs.getString("DEFECTION_NAME")));
                }

                dbGetDefectionsResponse.success = true;
                dbGetDefectionsResponse.defections = defections;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonOutput = gson.toJson(dbGetDefectionsResponse);

            } catch (SQLException e) {
                logError(CURRENT_FUNCTION, device_id, "Не удалось установить соединение с БД", e.toString());
                dbGetDefectionsResponse.success = false;
                dbGetDefectionsResponse.errorMessage = "Не удалось установить соединение с БД";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbGetDefectionsResponse);
            } catch (Exception e) {
                logError(CURRENT_FUNCTION, device_id, "Ошибка", e.toString());
                dbGetDefectionsResponse.success = false;
                dbGetDefectionsResponse.errorMessage = "Неизвестная ошибка";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbGetDefectionsResponse);
            }
        } catch (Exception e) {
            logError(CURRENT_FUNCTION, device_id, "Не удалось открыть источник данных из Context.xml", e.toString());
            dbGetDefectionsResponse.success = false;
            dbGetDefectionsResponse.errorMessage = "Не удалось открыть источник данных из Context.xml";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(dbGetDefectionsResponse);
        }

        log(CURRENT_FUNCTION, device_id, "Результат работы функции: " + JsonOutput);
        return JsonOutput;
    }

    /**
     * @t Function
     * @description Получение планов обхода из базы данных
     * @param official_id Уникальный идентификатор уполномоченного лица
     * @param represent_id Уникальный идентификатор подразделения
     * @param device_id Уникальный идентификатор устройства (указывается при логировании)
     * @return Планы обхода с подконтрольными объектами из базы данных (класс DatabaseGetPlansResponse в виде JSON строки)
     */
    public String getPlans(
            @WebParam(name = "official_id") String official_id,
            @WebParam(name = "represent_id") String represent_id,
            @WebParam(name = "device_id") String device_id) {

        String CURRENT_FUNCTION = new Throwable().getStackTrace()[0].getMethodName();
        log(CURRENT_FUNCTION, device_id, "Вызван метод");

        String JsonOutput = ""; // Результат загрузки планов в виде JSON строки

        DatabaseGetPlansResponse dbGetPlansResponse = new DatabaseGetPlansResponse();

        DataSource ds;
        try {
            // Инициализация контекста и источника данных
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/oracle11g");

            // Определение текущей даты
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            String currentDate = df.format(Calendar.getInstance().getTime());

            // TODO: DEBUG
            //currentDate = device_id;

            // SQL запросы к БД
            // Получение PLAN_ID, PLAN_CONTENT по OFFICIAL_ID и REPRESENT_ID из таблицы T_KT_PLAN
            String query1 = "SELECT PLAN_ID, PLAN_CONTENT FROM T_KT_PLAN WHERE OFFICIAL_ID=(?) AND REPRESENT_ID=(?) AND trunc(PLAN_DATE) = TO_DATE('" + currentDate + "','dd-mm-yyyy')";

            // Получение sub_control_id по plan_id из таблицы t_kt_sub_control
            String query2 = "SELECT SUB_CONTROL_ID, ADDRESS_ID, OBJECT_TYPE, CAD_NUMBER, ADDRESS_PLACE FROM T_KT_SUB_CONTROL WHERE PLAN_ID=(?)";

            // Получение PROPERTY_LABEL по KEY из таблицы S_KLS_D
            String query3 = "SELECT PROPERTY_LABEL FROM S_KLS_D WHERE KEY=(?)";

            // Получение ELEMENT_ADR_ID по address_id из таблицы tv_address_o
            String query4 = "SELECT ELEMENT_ADR_ID FROM TV_ADDRESS_O WHERE ADDRESS_ID=(?)";

            // Получение street_name по address_id
            String query5 = "SELECT ELEMENT_NAME FROM TV_STREET_O WHERE STREET_ID=(SELECT STREET_ID FROM TV_HOUSE_O WHERE HOUSE_ID=(?))";

            // Получение element_number, element_label по house_id
            String query6 = "SELECT ELEMENT_NUMBER, ELEMENT_LABEL, ELEMENT_NAME FROM TV_HOUSE_O WHERE HOUSE_ID=(?)";
            
            // Получение типа объекта для объектов без адреса
            String query7 = "SELECT PROPERTY_LABEL FROM S_KLS_D WHERE KEY=(?)";

            // Подключение к БД и загрузка данных
            try (Connection con = ds.getConnection();
                    PreparedStatement ps1 = con.prepareStatement(query1);
                    PreparedStatement ps2 = con.prepareStatement(query2);
                    PreparedStatement ps3 = con.prepareStatement(query3);
                    PreparedStatement ps4 = con.prepareStatement(query4);
                    PreparedStatement ps5 = con.prepareStatement(query5);
                    PreparedStatement ps6 = con.prepareStatement(query6);
                    PreparedStatement ps7 = con.prepareStatement(query7);) {

                // Query1 | Получение password, last_name, first_name, second_name, user_id по login из таблицы USER_TABLE
                ps1.setString(1, official_id);
                ps1.setString(2, represent_id);

                ResultSet rs1 = ps1.executeQuery();

                // Не найдено записей с планами
                if (!rs1.isBeforeFirst()) {
                    log(CURRENT_FUNCTION, device_id, "Не найдено планов в БД");
                    dbGetPlansResponse.success = false;
                    dbGetPlansResponse.errorMessage = "Не найдено планов в БД";
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    return gson.toJson(dbGetPlansResponse);
                }

                // Создаём объект с планами, загруженными из БД
                List<Plan> plans = new ArrayList();
                while (rs1.next()) {
                    plans.add(new Plan(rs1.getString("PLAN_ID"), rs1.getString("PLAN_CONTENT")));
                }

                for (int i = 0; i < plans.size(); i++) {
                    
                    // Query2 | Получение sub_control_id по plan_id из таблицы t_kt_sub_control
                    ps2.setString(1, plans.get(i).getId());

                    ResultSet rs2 = ps2.executeQuery();

                    // Добавляем данные объектов к планам 
                    List<PObject> tmp_pobjects = new ArrayList();
                    while (rs2.next()) {
                        tmp_pobjects.add(new PObject(rs2.getString("SUB_CONTROL_ID"), rs2.getString("ADDRESS_ID"),
                                rs2.getString("OBJECT_TYPE"), rs2.getString("CAD_NUMBER"), rs2.getString("ADDRESS_PLACE")));
                    }
                    plans.get(i).pobjects = tmp_pobjects;
                }
                
                // Добавляем данные объектов к планам
                for (int i = 0; i < plans.size(); i++) {
                    for (int j = 0; j < plans.get(i).pobjects.size(); j++) {
                        
                        // Query3 | Получение PROPERTY_LABEL из таблицы S_KLS_D
                        ps3.setString(1, plans.get(i).pobjects.get(j).object_type);

                        ResultSet rs3 = ps3.executeQuery();
                        if (!rs3.isBeforeFirst()) {
                            plans.get(i).pobjects.get(j).property_label = "Тип объекта не указан";
                        }
                        while (rs3.next()) {
                            plans.get(i).pobjects.get(j).property_label = rs3.getString("PROPERTY_LABEL");
                        }

                        // Query4 | Получение ELEMENT_ADR_ID по address_id из таблицы tv_address_o
                        ps4.setString(1, plans.get(i).pobjects.get(j).address_id);

                        ResultSet rs4 = ps4.executeQuery();
                        while (rs4.next()) {

                            plans.get(i).pobjects.get(j).element_adr_id = rs4.getString("ELEMENT_ADR_ID");
                        }

                        // Query5 | Получение street_name по address_id
                        ps5.setString(1, plans.get(i).pobjects.get(j).element_adr_id);

                        ResultSet rs5 = ps5.executeQuery();
                        while (rs5.next()) {
                            plans.get(i).pobjects.get(j).element_name = rs5.getString("ELEMENT_NAME");
                        }

                        // Query6 | Получение element_number, element_label по house_id
                        ps6.setString(1, plans.get(i).pobjects.get(j).element_adr_id);

                        ResultSet rs6 = ps6.executeQuery();
                        while (rs6.next()) {
                            plans.get(i).pobjects.get(j).helement_number = rs6.getString("ELEMENT_NUMBER");
                            plans.get(i).pobjects.get(j).helement_label = rs6.getString("ELEMENT_LABEL");
                            plans.get(i).pobjects.get(j).helement_name = rs6.getString("ELEMENT_NAME");
                        }

                        // Query7 | Получение типа объекта для объектов без адреса
                        if (plans.get(i).pobjects.get(j).address_id == null) {

                            ps7.setString(1, plans.get(i).pobjects.get(j).object_type);

                            ResultSet rs7 = ps7.executeQuery();
                            while (rs7.next()) {
                                plans.get(i).pobjects.get(j).object_type = rs7.getString("PROPERTY_LABEL");
                            }
                        }
                    }
                }

                // Оставить планы только с объектами
                List<Plan> plans_with_objects = new ArrayList();
                for (int i = 0; i < plans.size(); i++) {
                    if (plans.get(i).pobjects.size() > 0) {
                        plans_with_objects.add(plans.get(i));
                    } else {
                        System.out.println("Пропущен план без объектов. id=" + plans.get(i).id + " Название: " + plans.get(i).content);
                    }
                }

                // Есть планы с объектами
                if (plans_with_objects.size() > 0) {
                    dbGetPlansResponse.success = true;
                    dbGetPlansResponse.plans = plans_with_objects;
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonOutput = gson.toJson(dbGetPlansResponse);
                } else {
                    dbGetPlansResponse.success = false;
                    dbGetPlansResponse.errorMessage = "Не найдено планов в БД";
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    return gson.toJson(dbGetPlansResponse);
                }

            } catch (SQLException e) {
                logError(CURRENT_FUNCTION, device_id, "Не удалось установить соединение с БД", e.toString());
                dbGetPlansResponse.success = false;
                dbGetPlansResponse.errorMessage = "Не удалось установить соединение с БД";
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbGetPlansResponse);
            } catch (Exception e) {
                logError(CURRENT_FUNCTION, device_id, "Ошибка", e.toString());
                dbGetPlansResponse.success = false;
                dbGetPlansResponse.errorMessage = "Неизвестная ошибка " + e; // TODO: DEBUG
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(dbGetPlansResponse);
            }
        } catch (Exception e) {
            logError(CURRENT_FUNCTION, device_id, "Не удалось открыть источник данных из Context.xml", e.toString());
            dbGetPlansResponse.success = false;
            dbGetPlansResponse.errorMessage = "Не удалось открыть источник данных из Context.xml";
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(dbGetPlansResponse);
        }

        log(CURRENT_FUNCTION, device_id, "Результат работы функции: " + JsonOutput);
        return JsonOutput;
    }
}
