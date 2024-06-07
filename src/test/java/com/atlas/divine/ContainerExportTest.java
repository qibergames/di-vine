package com.atlas.divine;

import com.atlas.divine.descriptor.generic.Service;
import com.atlas.divine.descriptor.generic.ServiceScope;
import com.atlas.divine.tree.ContainerRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerExportTest {
    @Service(scope = ServiceScope.CONTAINER)
    public static class LocalService {
        public int foo() {
            return 123;
        }
    }

    @Service(scope = ServiceScope.SINGLETON)
    public static class GlobalService {
        public int bar() {
            return 456;
        }
    }

    @Test
    public void test_container_export() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        ContainerRegistry example = Container.of("example");
        LocalService localService = example.get(LocalService.class);
        GlobalService globalService = example.get(GlobalService.class);

        assertEquals(123, localService.foo());
        assertEquals(456, globalService.bar());

        ContainerRegistry global = Container.ofGlobal();
        global.set("MY_TOKEN", "Example Value");

        JsonObject json = Container.export();
        System.out.println(gson.toJson(json));

        assertEquals(2, json.get("totalDependencies").getAsInt());
        assertEquals(2, json.get("totalContainers").getAsInt());

        assertEquals(
            "Example Value",
            json
                .getAsJsonObject("global")
                .getAsJsonObject("values")
                .get("MY_TOKEN")
                .getAsString()
        );
    }
}
