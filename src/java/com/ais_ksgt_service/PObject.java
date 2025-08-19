package com.ais_ksgt_service;

import java.util.List;

public class PObject {

    public String sub_control_id; // Идентификатор записи
    public String address_id; // Адрес дома
    public String object_type; // Тип объекта
    public String property_label; // Сокращённое название
    public String cad_number; // Кадастровый номер
    public String address_place; // Местонахождение
    public String element_adr_id; // Ссылка на элемент адреса
    public String element_name; // Наименование улицы
    public String helement_number; // Номер дома
    public String helement_label; // Метка дома
    public String helement_name; // Наименование дома
    public List<Delict> delicts; // Нарушения
    public String photo_panorama_base64; // Фото панорамы (закодировано в Base64)
    public String photo_house_number_base64; // Фото номера дома (закодировано в Base64)

    // Конструктор для загрузки планов на устройство
    PObject(String sub_control_id, String address_id, String object_type, String cad_number, String address_place) {
        this.sub_control_id = sub_control_id;
        this.address_id = address_id;
        this.object_type = object_type;
        this.cad_number = cad_number;
        this.address_place = address_place;
    }

    // Конструктор для выгрузки объектов с устройства
    public PObject(String sub_control_id, String address_id, String photo_panorama_base64, String photo_house_number_base64, List<Delict> delicts) {
        this.sub_control_id = sub_control_id;
        this.address_id = address_id;
        this.photo_panorama_base64 = photo_panorama_base64;
        this.photo_house_number_base64 = photo_house_number_base64;
        this.delicts = delicts;
    }

    // Конструктор для выгрузки объектов с устройства
    public PObject(String sub_control_id, String address_id, String photo_panorama_base64, String photo_house_number_base64) {
        this.sub_control_id = sub_control_id;
        this.address_id = address_id;
        this.photo_panorama_base64 = photo_panorama_base64;
        this.photo_house_number_base64 = photo_house_number_base64;
    }
}
