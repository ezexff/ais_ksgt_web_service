package com.ais_ksgt_service;

import java.util.List;

public class DatabaseGetPlansResponse {

    public boolean success; // Успешное извлечение информации из базы данных
    public String errorMessage; // Сообщение об ошибке
    public List<Plan> plans; // Планы обхода
}
