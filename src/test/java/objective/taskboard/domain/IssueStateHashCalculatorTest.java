package objective.taskboard.domain;

import static org.junit.Assert.assertTrue;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.NeverWantedButInvoked;
import org.mockito.exceptions.verification.TooManyActualInvocations;
import org.mockito.exceptions.verification.WantedButNotInvoked;

import com.fasterxml.jackson.annotation.JsonIgnore;

import objective.taskboard.data.Issue;

public class IssueStateHashCalculatorTest {

    @Test
    public void verifyIfAllMethodsUsedByJsonConversionAreConsideredInHashCalculation() throws IllegalAccessException, IntrospectionException {
        // given
        Issue issue = Mockito.mock(Issue.class);
        IssueStateHashCalculator issueStateHashCalculator = new IssueStateHashCalculator();

        // when
        issueStateHashCalculator.calculateHash(issue);

        // then
        assertTrue(gettersWereInvokedOnlyOnce(issue));
    }

    private static boolean gettersWereInvokedOnlyOnce(Issue issue) throws IntrospectionException {
        MockingDetails details = Mockito.mockingDetails(issue);

        Map<String, Long> invocationCount = details.getInvocations()
                .stream()
                .collect(Collectors.groupingBy(
                        invocation -> invocation.getMethod().getName()
                        , Collectors.counting()));

        for(Method getter : getters()) {
            String methodName = getter.getName();
            if(!invocationCount.containsKey(methodName))
                throw new WantedButNotInvoked(methodName);
            long count = invocationCount.remove(methodName);
            if(count > 1)
                throw new TooManyActualInvocations(methodName);
        }

        for(String extraMethod : invocationCount.keySet()) {
            throw new NeverWantedButInvoked(extraMethod);
        }

        return true;
    }

    private static List<Method> getters() throws IntrospectionException {
        PropertyDescriptor[] allProperties = Introspector.getBeanInfo(Issue.class).getPropertyDescriptors();

        Set<String> ignoredFields = FieldUtils.getFieldsListWithAnnotation(Issue.class, JsonIgnore.class)
                .stream()
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        Set<String> ignoredMethods = MethodUtils.getMethodsListWithAnnotation(Issue.class, JsonIgnore.class)
                .stream()
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        // these fields are ignored because they are computed out of other data
        ignoredMethods.add("getRawAssignedTeamsIds");
        ignoredMethods.add("getCardTshirtSize");
        ignoredMethods.add("getMismatchingUsers");
        ignoredMethods.add("getAssignees");
        ignoredMethods.add("isDemand");
        ignoredMethods.add("isFeature");
        ignoredMethods.add("isSubTask");
        ignoredMethods.add("getParentSummary");
        ignoredMethods.add("getTeamNames");
        ignoredMethods.add("getTshirtSizes");
        ignoredMethods.add("isUsingDefaultTeam");
        ignoredMethods.add("isUsingParentTeam");
        
        ignoredMethods.add("getStateHash");
        ignoredMethods.add("getClass");

        return Arrays.stream(allProperties)
                .filter(prop -> ignoredFields.stream().noneMatch(prop.getName()::equalsIgnoreCase))
                .map(prop -> prop.getReadMethod())
                .filter(method -> !ignoredMethods.contains(method.getName()))
                .collect(Collectors.toList());
    }
}
