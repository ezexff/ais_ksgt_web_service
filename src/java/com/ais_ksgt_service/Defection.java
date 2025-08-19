package com.ais_ksgt_service;

public class Defection {

    public String id; // Уникальный идентификатор
    public String label; // Краткое наименование нарушения
    public String name; // Полное наименование нарушения

    // Конструктор для загрузки видов нарушений на устройство
    Defection(String id, String label, String name) {
        this.id = id;
        this.label = label;
        this.name = name;
    }
}
