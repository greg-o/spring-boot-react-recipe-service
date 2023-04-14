package org.grego.recipeservice;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests for Recipe Service")
@SelectPackages("org.grego.recipeservice")
@IncludeTags("IntegrationTests")
public class IntegrationTestsSuite {
}
