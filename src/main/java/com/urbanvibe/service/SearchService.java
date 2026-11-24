package com.urbanvibe.service;

import com.urbanvibe.dao.ExperienceDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.model.Experience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchService implements Searchable {

    private final ExperienceDAO experienceDAO;

    public SearchService(ExperienceDAO experienceDAO) {
        this.experienceDAO = experienceDAO;
    }

    @Override
    public List<Experience> search(String keyword) throws DatabaseOperationException {
        String key = keyword.trim().toLowerCase();
        List<Experience> result = new ArrayList<>();
        for (Experience e : experienceDAO.findAll(true)) {
            if (e.getTitle().toLowerCase().contains(key) || e.getCity().toLowerCase().contains(key)
                    || e.getVenue().toLowerCase().contains(key) || e.getCategory().toLowerCase().contains(key)) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Experience> search(String category, String city) throws DatabaseOperationException {
        List<Experience> result = new ArrayList<>();
        for (Experience e : experienceDAO.findAll(true)) {
            boolean categoryOk = category.isBlank() || e.getCategory().equalsIgnoreCase(category.trim());
            boolean cityOk = city.isBlank() || e.getCity().equalsIgnoreCase(city.trim());
            if (categoryOk && cityOk) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Experience> search(double minPrice, double maxPrice)
            throws DatabaseOperationException, InvalidInputException {
        if (minPrice < 0 || maxPrice < minPrice) {
            throw new InvalidInputException("Price range is invalid (min must be 0 or more and not above max).");
        }
        List<Experience> result = new ArrayList<>();
        for (Experience e : experienceDAO.findAll(true)) {
            if (e.getBasePrice() >= minPrice && e.getBasePrice() <= maxPrice) {
                result.add(e);
            }
        }
        return result;
    }

    public void sortByPrice(List<Experience> list) {
        Collections.sort(list, new Comparator<Experience>() {
            @Override
            public int compare(Experience e1, Experience e2) {
                return Double.compare(e1.getBasePrice(), e2.getBasePrice());
            }
        });
    }

    public void sortByTitle(List<Experience> list) {
        Collections.sort(list, new Comparator<Experience>() {
            @Override
            public int compare(Experience e1, Experience e2) {
                return e1.getTitle().toLowerCase().compareTo(e2.getTitle().toLowerCase());
            }
        });
    }
}
