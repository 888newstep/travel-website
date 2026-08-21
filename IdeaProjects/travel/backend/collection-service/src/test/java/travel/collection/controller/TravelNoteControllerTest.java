package travel.collection.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import travel.collection.service.TravelNoteService;
import travel.common.entity.user_community.TravelNote;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TravelNoteControllerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private TravelNoteService travelNoteService;

    @InjectMocks
    private TravelNoteController controller;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void shouldBindTypedRequestAndUseAuthenticatedUser() throws Exception {
        authenticate(42L);
        when(travelNoteService.createTravelNote(eq(42), any(TravelNote.class), eq(List.of("city"))))
                .thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(post("/travel-notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "travelNote": {
                                    "id": 99,
                                    "userId": 999,
                                    "title": "Shanghai",
                                    "content": "A complete travel note",
                                    "cityId": 1,
                                    "isPublic": true
                                  },
                                  "tags": ["city"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Shanghai"));

        ArgumentCaptor<TravelNote> noteCaptor = ArgumentCaptor.forClass(TravelNote.class);
        verify(travelNoteService).createTravelNote(eq(42), noteCaptor.capture(), eq(List.of("city")));
        assertNull(noteCaptor.getValue().getId());
        assertNull(noteCaptor.getValue().getUserId());
        assertEquals("Shanghai", noteCaptor.getValue().getTitle());
    }

    @Test
    void shouldRejectMissingTitleBeforeCallingService() throws Exception {
        authenticate(42L);

        mockMvc.perform(post("/travel-notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"travelNote":{"content":"content"}}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelNoteService);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
