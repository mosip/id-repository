package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

/**
 * 
 * @author Nagarjuna
 *
 * @param <T>
 */
@Data
public class EventModel<T> {

    /** The publisher. */
    private String publisher;
    
    /** The topic. */
    private String topic;
    
    /** The published on. */
    private String publishedOn;
    
    /** The event. */
    private T event;
    
    /** The data. */
    private Map<String, Object> data;
}
