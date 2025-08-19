package com.ais_ksgt_service;

import java.util.List;

public class DatabaseLoginResponse {

    boolean success; // Успешная авторизация
    String errorMessage; // Сообщение об ошибке
    String user_id; // Идентификатор пользователя
    String first_name; // Имя уполномоченного лица
    String second_name; // Отчество уполномоченного лица
    String last_name; // Фамилия уполномоченного лица
    String webServiceDate; // Дата, установленная на компьютере веб-сервиса
    List<Represent> represents; // Подразделения
}
