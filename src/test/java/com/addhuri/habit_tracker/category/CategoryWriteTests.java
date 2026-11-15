package com.addhuri.habit_tracker.category;

import com.addhuri.habit_tracker.TestTokens;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;
import com.addhuri.habit_tracker.category.repository.CategoryRepository;
import com.addhuri.habit_tracker.security.AuthTokenProperties;
import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryWriteTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private AuthTokenProperties auth;

	@Autowired
	private UserRepository users;

	@Autowired
	private CategoryRepository categories;

	@Test
	void rejectsMissingToken() throws Exception {
		mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Health\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void createsRootAndChild() throws Exception {
		User user = saveUser();
		UUID rootId = createCategory(user, """
				{"name":"  Health  ","description":"   "}
				""");

		getCategory(user, rootId).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("OK"))
				.andExpect(jsonPath("$.data.id").value(rootId.toString()))
				.andExpect(jsonPath("$.data.name").value("Health"))
				.andExpect(jsonPath("$.data.description").value(nullValue()))
				.andExpect(jsonPath("$.data.parentId").value(nullValue()))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.createdAt").exists())
				.andExpect(jsonPath("$.data.updatedAt").exists());

		UUID childId = createCategory(user, """
				{"name":"Cardio","description":"  Heart health  ","parentId":"%s"}
				""".formatted(rootId));

		getCategory(user, childId).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Cardio"))
				.andExpect(jsonPath("$.data.description").value("Heart health"))
				.andExpect(jsonPath("$.data.parentId").value(rootId.toString()))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));

		mvc.perform(get("/api/categories").header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].id").value(childId.toString()))
				.andExpect(jsonPath("$.data[1].id").value(rootId.toString()));
	}

	@Test
	void rejectsInvalidCategoryFields() throws Exception {
		User user = saveUser();
		postCategory(user, "{\"name\":\"   \"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
		postCategory(user, "{\"name\":\"" + "a".repeat(101) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
		postCategory(user, "{\"name\":\"Health\",\"description\":\"" + "a".repeat(501) + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void rejectsDuplicateName() throws Exception {
		User user = saveUser();
		createCategory(user, "{\"name\":\"Health\"}");

		postCategory(user, "{\"name\":\"  Health  \"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A category with this name already exists"));
		postCategory(user, "{\"name\":\"health\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A category with this name already exists"));
	}

	@Test
	void rejectsNameHeldByDeletedCategory() throws Exception {
		User user = saveUser();
		UUID id = createCategory(user, "{\"name\":\"Health\"}");
		deleteCategory(user, id).andExpect(status().isOk());

		postCategory(user, "{\"name\":\"Health\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A category with this name already exists"));
	}

	@Test
	void allowsTheSameNameUnderAnotherParent() throws Exception {
		User user = saveUser();
		UUID healthId = createCategory(user, "{\"name\":\"Health\"}");
		UUID otherId = createCategory(user, "{\"name\":\"Other\"}");
		UUID childId = createCategory(user, """
				{"name":"Health","parentId":"%s"}
				""".formatted(otherId));

		getCategory(user, childId).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.parentId").value(otherId.toString()));

		patchCategory(user, childId, "{\"parentId\":null}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A category with this name already exists"));
		getCategory(user, childId).andExpect(jsonPath("$.data.parentId").value(otherId.toString()));
		getCategory(user, healthId).andExpect(jsonPath("$.data.name").value("Health"));
	}

	@Test
	void rejectsAParentTheCallerCannotUse() throws Exception {
		User user = saveUser();
		User other = saveUser();
		UUID otherCategoryId = createCategory(other, "{\"name\":\"Theirs\"}");

		postCategory(user, """
				{"name":"Health","parentId":"%s"}
				""".formatted(UUID.randomUUID())).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Parent category not found"));
		postCategory(user, """
				{"name":"Health","parentId":"%s"}
				""".formatted(otherCategoryId)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Parent category not found"));
	}

	@Test
	void rejectsACycle() throws Exception {
		User user = saveUser();
		UUID rootId = createCategory(user, "{\"name\":\"Health\"}");
		UUID childId = createCategory(user, """
				{"name":"Cardio","parentId":"%s"}
				""".formatted(rootId));
		UUID grandchildId = createCategory(user, """
				{"name":"Running","parentId":"%s"}
				""".formatted(childId));

		patchCategory(user, rootId, """
				{"parentId":"%s"}
				""".formatted(grandchildId)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Parent category would create a cycle"));
		patchCategory(user, childId, """
				{"parentId":"%s"}
				""".formatted(childId)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Parent category would create a cycle"));
		getCategory(user, rootId).andExpect(jsonPath("$.data.parentId").value(nullValue()));
	}

	@Test
	void hidesCategoriesTheCallerCannotSee() throws Exception {
		User user = saveUser();
		User other = saveUser();
		UUID id = createCategory(user, "{\"name\":\"Health\",\"description\":\"Daily\"}");
		UUID otherId = createCategory(other, "{\"name\":\"Work\"}");

		getCategory(user, id).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Health"))
				.andExpect(jsonPath("$.data.description").value("Daily"));
		getCategory(user, otherId).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Category not found"));
		getCategory(user, UUID.randomUUID()).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Category not found"));

		deleteCategory(user, id).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DELETED"));
		getCategory(user, id).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Category not found"));
	}

	@Test
	void leavesOmittedFieldsUnchanged() throws Exception {
		User user = saveUser();
		UUID rootId = createCategory(user, "{\"name\":\"Health\"}");
		UUID childId = createCategory(user, """
				{"name":"Cardio","description":"Heart health","parentId":"%s"}
				""".formatted(rootId));

		patchCategory(user, childId, "{\"name\":\"  Running  \"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Running"))
				.andExpect(jsonPath("$.data.description").value("Heart health"))
				.andExpect(jsonPath("$.data.parentId").value(rootId.toString()))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	void clearsParentAndDescription() throws Exception {
		User user = saveUser();
		UUID rootId = createCategory(user, "{\"name\":\"Health\"}");
		UUID childId = createCategory(user, """
				{"name":"Cardio","description":"Heart health","parentId":"%s"}
				""".formatted(rootId));

		patchCategory(user, childId, "{\"parentId\":null,\"description\":null}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Cardio"))
				.andExpect(jsonPath("$.data.parentId").value(nullValue()))
				.andExpect(jsonPath("$.data.description").value(nullValue()));
	}

	@Test
	void rejectsABlankRename() throws Exception {
		User user = saveUser();
		UUID id = createCategory(user, "{\"name\":\"Health\"}");

		patchCategory(user, id, "{\"name\":\"   \"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Name is required"));
		getCategory(user, id).andExpect(jsonPath("$.data.name").value("Health"));
	}

	@Test
	void changesStatus() throws Exception {
		User user = saveUser();
		UUID id = createCategory(user, "{\"name\":\"Health\",\"description\":\"Daily\"}");

		patchStatus(user, id, "INACTIVE").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("INACTIVE"));
		patchStatus(user, id, "ACTIVE").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));
		patchStatus(user, id, "ARCHIVED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"));
		patchCategory(user, id, "{\"description\":\"Kept\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"))
				.andExpect(jsonPath("$.data.description").value("Kept"));
	}

	@Test
	void rejectsIllegalStatusChanges() throws Exception {
		User user = saveUser();
		UUID id = createCategory(user, "{\"name\":\"Health\"}");

		patchStatus(user, id, "ACTIVE").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Category already has this status"));
		patchStatus(user, id, "DELETED").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Status cannot be set to DELETED"));

		patchStatus(user, id, "ARCHIVED").andExpect(status().isOk());
		patchStatus(user, id, "ACTIVE").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Archived category cannot change status"));
		patchStatus(user, id, "INACTIVE").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Archived category cannot change status"));
		getCategory(user, id).andExpect(jsonPath("$.data.status").value("ARCHIVED"));
	}

	@Test
	void softDeletesAndHidesTheCategory() throws Exception {
		User user = saveUser();
		UUID keptId = createCategory(user, "{\"name\":\"Health\"}");
		UUID deletedId = createCategory(user, "{\"name\":\"Work\"}");

		deleteCategory(user, deletedId).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("OK"))
				.andExpect(jsonPath("$.data.id").value(deletedId.toString()))
				.andExpect(jsonPath("$.data.status").value("DELETED"));

		assertEquals(CategoryStatus.DELETED, categories.findById(deletedId).orElseThrow().getStatus());
		getCategory(user, deletedId).andExpect(status().isNotFound());
		mvc.perform(get("/api/categories").header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(keptId.toString()));
	}

	@Test
	void rejectsDeleteWhileAChildRemains() throws Exception {
		User user = saveUser();
		UUID rootId = createCategory(user, "{\"name\":\"Health\"}");
		UUID childId = createCategory(user, """
				{"name":"Cardio","parentId":"%s"}
				""".formatted(rootId));

		deleteCategory(user, rootId).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Category has child categories"));
		getCategory(user, rootId).andExpect(jsonPath("$.data.status").value("ACTIVE"));

		deleteCategory(user, childId).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DELETED"));
		deleteCategory(user, rootId).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DELETED"));
	}

	private UUID createCategory(User user, String body) throws Exception {
		String json = postCategory(user, body).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("OK"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		return UUID.fromString(JsonPath.read(json, "$.data.id"));
	}

	private ResultActions postCategory(User user, String body) throws Exception {
		return mvc.perform(post("/api/categories").header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions getCategory(User user, UUID id) throws Exception {
		return mvc.perform(get("/api/categories/" + id).header("Authorization", bearer(user)));
	}

	private ResultActions patchCategory(User user, UUID id, String body) throws Exception {
		return mvc.perform(patch("/api/categories/" + id).header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions patchStatus(User user, UUID id, String status) throws Exception {
		return mvc.perform(patch("/api/categories/" + id + "/status").header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"" + status + "\"}"));
	}

	private ResultActions deleteCategory(User user, UUID id) throws Exception {
		return mvc.perform(delete("/api/categories/" + id).header("Authorization", bearer(user)));
	}

	private String bearer(User user) {
		return TestTokens.bearer(auth.jwtSecret(), user.getId());
	}

	private User saveUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setAccountStatus(AccountStatus.ACTIVE);
		return users.saveAndFlush(user);
	}

}
