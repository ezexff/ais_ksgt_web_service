package com.ais_ksgt_service;

import java.util.List;

public class DatabaseGetDefectionsResponse {

    public boolean success; // Успешное извлечение информации из базы данных
    public String errorMessage; // Сообщение об ошибке
    List<Defection> defections; // Виды нарушений
}
