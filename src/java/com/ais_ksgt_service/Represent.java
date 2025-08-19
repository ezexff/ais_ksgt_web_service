package com.ais_ksgt_service;

public class Represent {

    String id; // Идентификатор подразделения
    String official_id; // Идентификатор уполномоченного лица
    String label; // Наименование подразделения

    // Конструктор для загрузки списка подразделений (после авторизации) на устройство
    Represent(String id, String official_id) {
        this.id = id;
        this.official_id = official_id;
    }

    String getId() {
        return this.id;
    }

    void setLabel(String label) {
        this.label = label;
    }
}
