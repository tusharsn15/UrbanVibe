package com.urbanvibe.service;

import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.model.Experience;

import java.util.List;

public interface Searchable {
    List<Experience> search(String keyword) throws DatabaseOperationException;
}
