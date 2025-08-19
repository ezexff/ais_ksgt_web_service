package com.ais_ksgt_service;

public class Delict {

    public String defection_id; // Идентификатор вида нарушения
    public String defection_name; // Полное наименование нарушения
    public String time; // Время нарушения
    public String photo_base64; // Фото нарушения (закодировано в Base64)

    // Конструктор для выгрузки нарушений с устройства
    public Delict(String defection_id, String defection_name, String time, String photo_base64) {
        this.defection_id = defection_id;
        this.defection_name = defection_name;
        this.time = time;
        this.photo_base64 = photo_base64;
    }
}
