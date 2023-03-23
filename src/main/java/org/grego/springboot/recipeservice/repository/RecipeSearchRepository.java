package org.grego.springboot.recipeservice.repository;

import org.grego.springboot.recipeservice.document.RecipeDoc;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeSearchRepository extends ReactiveElasticsearchRepository<RecipeDoc, Long> {

}
