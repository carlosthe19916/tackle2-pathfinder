package io.tackle.pathfinder.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.tackle.pathfinder.AbstractResourceTest;
import io.tackle.pathfinder.PathfinderTestProfile;
import io.tackle.pathfinder.dto.AssessmentCategoryDto;
import io.tackle.pathfinder.dto.AssessmentDto;
import io.tackle.pathfinder.dto.AssessmentHeaderDto;
import io.tackle.pathfinder.dto.AssessmentQuestionnaireDto;
import io.tackle.pathfinder.dto.translator.TranslateDBDto;
import io.tackle.pathfinder.model.questionnaire.Category;
import io.tackle.pathfinder.model.questionnaire.Questionnaire;
import io.tackle.pathfinder.services.AssessmentSvc;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(PathfinderTestProfile.class)
@Log
class TranslatorResourceTest extends AbstractResourceTest {

    @Inject
    AssessmentSvc assessmentSvc;

    @Test
    public void given_Translation_When_Posted_Then_ReturnsContentDto() {
        // Given
        AssessmentHeaderDto assessmentHeaderDto = assessmentSvc.newAssessment(null, 100L);
        AssessmentDto assessmentDto = given()
                .queryParam("applicationId", "20")
                .when()
                .get("/assessments/" + assessmentHeaderDto.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(AssessmentDto.class);

        AssessmentQuestionnaireDto questionnaireDto = assessmentDto.getQuestionnaire();
        assertThat(questionnaireDto.getLanguage()).isEqualTo("EN");

        AssessmentCategoryDto categoryDto = questionnaireDto.getCategories().stream().filter(f -> f.getOrder().equals(3)).findFirst().orElse(null);
        assertThat(categoryDto).isNotNull();
        assertThat(categoryDto.getTitle()).isEqualTo("Application architecture");

        // When
        Questionnaire questionnaire = Questionnaire.findAll().firstResult();
        Category category = questionnaire.categories.stream().filter(a -> a.order == 3).findFirst().get();
        assertThat(category).extracting(a -> a.name).isEqualTo("Application architecture");

        TranslateDBDto translateDto = TranslateDBDto.builder()
                .id(category.id)
                .table("Category")
                .field("name")
                .text("Application architecture: in language ABC")
                .language("ABC")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(translateDto)
                .when()
                .post("/translator/translate")
                .then()
                .statusCode(201);

        // Then
        assessmentDto = given()
                .queryParam("applicationId", "20")
                .when()
                .get("/assessments/" + assessmentHeaderDto.getId() + "?language=ABC")
                .then()
                .statusCode(200)
                .extract()
                .as(AssessmentDto.class);

        questionnaireDto = assessmentDto.getQuestionnaire();
        assertThat(questionnaireDto.getLanguage()).isEqualTo("ABC");

        categoryDto = questionnaireDto.getCategories().stream().filter(f -> f.getOrder().equals(3)).findFirst().orElse(null);
        assertThat(categoryDto).isNotNull();
        assertThat(categoryDto.getTitle()).isEqualTo("Application architecture: in language ABC");
    }
}
