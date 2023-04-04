/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.repository;

import org.grego.recipeservice.document.RecipeDoc;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * RecipeSearchRepository for managing Recipe documents in the search engine.
 */
@Repository
public interface RecipeSearchRepository extends ReactiveElasticsearchRepository<RecipeDoc, Long> {

}
