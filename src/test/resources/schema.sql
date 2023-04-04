CREATE TABLE IF NOT EXISTS ingredients (
	ingredient_id  BIGSERIAL NOT NULL,
    ingredient VARCHAR(256) NOT NULL,
    ingredient_number INT CONSTRAINT positive_ingredient_number CHECK (ingredient_number > 0),
    quantity DECIMAL(10,2) NOT NULL,
    quantity_specifier VARCHAR(255) NOT NULL,
    PRIMARY KEY (ingredient_id)
);

CREATE TABLE IF NOT EXISTS instructions (
	instruction_id  BIGSERIAL NOT NULL,
    instruction VARCHAR(255) NOT NULL,
    instruction_number INT CONSTRAINT positive_instruction_number CHECK (instruction_number > 0),
    PRIMARY KEY (instruction_id)
);

CREATE TABLE IF NOT EXISTS recipes (
    recipe_id  BIGSERIAL NOT NULL,
    creation_date_time TIMESTAMP NOT NULL,
    description VARCHAR(255) NOT NULL,
    last_modified_date_time TIMESTAMP NOT NULL,
    name VARCHAR(256) NOT NULL,
    variation INT4 NOT NULL,
    PRIMARY KEY (recipe_id)
);

CREATE TABLE IF NOT EXISTS recipes_ingredients (
	recipe_recipe_id INT8 NOT NULL,
    ingredients_ingredient_id INT8 NOT NULL
);

CREATE TABLE IF NOT EXISTS recipes_instructions (
	recipe_recipe_id INT8 NOT NULL,
    instructions_instruction_id INT8 NOT NULL
);

--ALTER TABLE recipes
--	DROP CONSTRAINT IF EXISTS unique_recipe_name_and_variation;
--
--ALTER TABLE recipes
--	ADD CONSTRAINT unique_recipe_name_and_variation UNIQUE (name, variation);
--
--ALTER TABLE recipes_ingredients
--	DROP CONSTRAINT IF EXISTS UK_hfvx94nbsiawh2p8gtyqu8ii4
--
--ALTER TABLE recipes_ingredients
--	ADD CONSTRAINT UK_hfvx94nbsiawh2p8gtyqu8ii4 UNIQUE (ingredients_ingredient_id)
--
--ALTER TABLE recipes_instructions
--	DROP CONSTRAINT IF EXISTS UK_frj0p2b8t86143j7wulj48gnt
--
--ALTER TABLE recipes_instructions
--	ADD CONSTRAINT UK_frj0p2b8t86143j7wulj48gnt UNIQUE (instructions_instruction_id)
--
--ALTER TABLE recipes_ingredients
--	ADD CONSTRAINT FKdgayn6mr8yqada1unqndvmtvh
--	FOREIGN KEY (ingredients_ingredient_id)
--	REFERENCES ingredients
--
--ALTER TABLE recipes_ingredients
--	ADD CONSTRAINT FK1mi8s2sq8it3qc7aj7osg9bvy
--	FOREIGN KEY (recipe_recipe_id)
--	REFERENCES recipes
--
--ALTER TABLE recipes_instructions
--	ADD CONSTRAINT FKc0q6029p52swaycmvoj1va5kc
--	FOREIGN KEY (instructions_instruction_id)
--	REFERENCES instructions
--
--ALTER TABLE recipes_instructions
--	ADD CONSTRAINT FK9yrisqx52gg4srspy0uligh3n
--	FOREIGN KEY (recipe_recipe_id)
--	REFERENCES recipes
