/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base class for Elasticsearch documents, contains _class field.
 */
@Getter
@Setter
@NoArgsConstructor
public class ElasticsearchDoc {
    /**
     * The class for the Elasticsearch document.
     */
    private String _class;
}
