package services;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.models.Page;
import com.looksee.models.PageState;
import com.looksee.models.repository.PageRepository;
import com.looksee.models.repository.PageStateRepository;
import com.looksee.models.repository.PerformanceInsightRepository;
import com.looksee.services.PageService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageStateRepository pageStateRepository;

    @Mock
    private PerformanceInsightRepository performanceInsightRepository;

    @InjectMocks
    private PageService pageService;

    @Test
    void saveForUserUpdatesExistingPageForSameUser() {
        String userId = "user-1";
        String pageKey = "page-key";

        Page existingPage = new Page();
        existingPage.setKey(pageKey);

        Page inputPage = new Page();
        inputPage.setKey(pageKey);
        inputPage.setPageStates(Collections.emptyList());

        when(pageRepository.findByKeyAndUser(userId, pageKey)).thenReturn(existingPage);
        when(pageRepository.save(existingPage)).thenReturn(existingPage);

        Page saved = pageService.saveForUser(userId, inputPage);

        assertSame(existingPage, saved);
        verify(pageRepository).save(existingPage);
    }

    @Test
    void saveForUserCreatesNewPageWhenUserScopedRecordDoesNotExist() {
        String userId = "user-1";
        String pageKey = "page-key";

        Page inputPage = new Page();
        inputPage.setKey(pageKey);

        when(pageRepository.findByKeyAndUser(userId, pageKey)).thenReturn(null);
        when(pageRepository.save(inputPage)).thenReturn(inputPage);

        Page saved = pageService.saveForUser(userId, inputPage);

        assertSame(inputPage, saved);
        verify(pageRepository).save(inputPage);
    }

    @Test
    void findLatestInsightReturnsNullWhenNoInsightsExist() {
        when(pageRepository.getLatestPerformanceInsight("page-key")).thenReturn(null);

        PerformanceInsight result = pageService.findLatestInsight("page-key");

        assertNull(result);
        verify(performanceInsightRepository, never()).getAllAudits(any(), any());
    }

    @Test
    void findLatestInsightHydratesAuditsWhenInsightExists() {
        PerformanceInsight insight = new PerformanceInsight();
        insight.setKey("insight-key");

        when(pageRepository.getLatestPerformanceInsight("page-key")).thenReturn(insight);
        when(performanceInsightRepository.getAllAudits("page-key", "insight-key")).thenReturn(Collections.emptyList());

        PerformanceInsight result = pageService.findLatestInsight("page-key");

        assertSame(insight, result);
        assertTrue(result.getAudits().isEmpty());
    }


    @Test
    void addPageStatePersistsStateAndPageWhenTargetPageExists() {
        String userId = "user-1";
        String pageKey = "page-key";

        Page page = new Page();
        page.setKey(pageKey);

        PageState pageState = new PageState();
        pageState.setKey("state-key");

        when(pageRepository.findByKeyAndUser(userId, pageKey)).thenReturn(page);
        when(pageStateRepository.findByKeyAndUsername(userId, "state-key")).thenReturn(null);
        when(pageStateRepository.save(pageState)).thenReturn(pageState);

        pageService.addPageState(userId, pageKey, pageState);

        verify(pageStateRepository).save(pageState);
        verify(pageRepository).save(page);
    }

    @Test
    void addPageStateSkipsSaveWhenTargetPageForUserIsMissing() {
        String userId = "user-1";
        String pageKey = "missing-page";

        PageState pageState = new PageState();
        pageState.setKey("state-key");

        when(pageRepository.findByKeyAndUser(userId, pageKey)).thenReturn(null);

        pageService.addPageState(userId, pageKey, pageState);

        verify(pageRepository, never()).save(any(Page.class));
        verify(pageStateRepository, never()).save(any(PageState.class));
    }
}
