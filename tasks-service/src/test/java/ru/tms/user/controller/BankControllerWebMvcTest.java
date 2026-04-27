package ru.tms.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.tms.user.controller.mapper.QuestionBankMapper;
import ru.tms.user.controller.support.OwnerIdResolver;
import ru.tms.user.controller.support.SearchQueryNormalizer;
import ru.tms.user.service.QuestionBankService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BankController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BankControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionBankService questionBankService;

    @MockBean
    private QuestionBankMapper questionBankMapper;

    @MockBean
    private OwnerIdResolver ownerIdResolver;

    @MockBean
    private SearchQueryNormalizer searchQueryNormalizer;

    @Test
    void shouldReturnBadRequestForUnsupportedDateFormat() throws Exception {
        when(ownerIdResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("teacher-1");

        mockMvc.perform(get("/tasks-service/banks")
                        .param("createdFrom", "2026/12/31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(questionBankService);
    }

    @Test
    void shouldReturnBadRequestWhenCreatedFromAfterCreatedTo() throws Exception {
        when(ownerIdResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("teacher-1");

        mockMvc.perform(get("/tasks-service/banks")
                        .param("createdFrom", "2026-12-31")
                        .param("createdTo", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(questionBankService);
    }
}

