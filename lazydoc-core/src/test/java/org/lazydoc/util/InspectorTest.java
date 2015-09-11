package org.lazydoc.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class InspectorTest {

    private Method methodWithSetResponse;
    private Method methodWithListResponse;
    private Method methodWithStringResponse;
    private Method methodWithStringArrayResponse;

    public List<String> methodWithListResponse() {
        return null;
    }

    public Set<String> methodWithSetResponse() {
        return null;
    }


    public String[] methodWithStringArrayResponse() {
        return null;
    }


    public String methodWithStringResponse() {
        return null;
    }


    @Before
    public void setup() throws NoSuchMethodException {
        methodWithListResponse = this.getClass().getDeclaredMethod("methodWithListResponse");
        methodWithStringResponse = this.getClass().getDeclaredMethod("methodWithStringResponse");
        methodWithStringArrayResponse = this.getClass().getDeclaredMethod("methodWithStringArrayResponse");
        methodWithSetResponse = this.getClass().getDeclaredMethod("methodWithSetResponse");
    }

    @Test
    public void testGetGenericClassOfList() throws Exception {
        assertThat(Inspector.getGenericClassOfList(methodWithListResponse.getReturnType(), methodWithListResponse.getGenericReturnType()).getName(), is("java.lang.String"));
    }

    @Test
    public void testGetGenericClassOfArray() throws Exception {
        assertThat(Inspector.getGenericClassOfList(methodWithStringArrayResponse.getReturnType(), methodWithStringArrayResponse.getGenericReturnType()).getName(), is("java.lang.String"));
    }

    @Test
    public void testGetGenericClassOfSet() throws Exception {
        assertThat(Inspector.getGenericClassOfList(methodWithSetResponse.getReturnType(), methodWithSetResponse.getGenericReturnType()).getName(), is("java.lang.String"));
    }

    @Test
    public void testGetNoGenericClassOfSet() throws Exception {
        assertThat(Inspector.getGenericClassOfList(methodWithStringResponse.getReturnType(), methodWithStringResponse.getGenericReturnType()), is(nullValue()));
    }


    @Test
    public void testIsList() throws Exception {
        assertThat(Inspector.isListSetOrArray(methodWithListResponse.getReturnType()), is(true));

    }

    @Test
    public void testIsArray() throws Exception {
        assertThat(Inspector.isListSetOrArray(methodWithStringArrayResponse.getReturnType()), is(true));
    }

    @Test
    public void testIsSet() throws Exception {
        assertThat(Inspector.isListSetOrArray(methodWithSetResponse.getReturnType()), is(true));
    }

    @Test
    public void testIsNotAListSetOrArray() throws Exception {
        assertThat(Inspector.isListSetOrArray(methodWithStringResponse.getReturnType()), is(false));
    }



}