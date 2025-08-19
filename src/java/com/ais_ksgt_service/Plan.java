package com.ais_ksgt_service;

import java.util.List;

public class Plan {

    public String id; // Идентификатор плана
    public String content; // Краткое описание
    public List<PObject> pobjects; // Подконтрольные объекты

    // Конструктор для загрузки планов на устройство
    Plan(String id, String content) {
        this.id = id;
        this.content = content;
    }

    // Получения идентификатора плана (используется при загрузке планов на устройство)
    public String getId() {
        return(this.id);
    }
}
