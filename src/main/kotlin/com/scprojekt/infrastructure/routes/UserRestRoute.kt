package com.scprojekt.infrastructure.routes

import com.scprojekt.domain.model.user.entity.User
import com.scprojekt.domain.model.user.entity.UserType
import com.scprojekt.infrastructure.constants.Routes.Companion.DIRECT_CREATEUSER
import com.scprojekt.infrastructure.constants.Routes.Companion.DIRECT_DELETEUSER
import com.scprojekt.infrastructure.constants.Routes.Companion.DIRECT_FINDBYUUID
import com.scprojekt.infrastructure.constants.Routes.Companion.DIRECT_SAVEINDATABASE
import com.scprojekt.infrastructure.constants.Routes.Companion.DRECT_MANAGEUSER
import com.scprojekt.infrastructure.constants.Routes.Companion.MEDIATYPE_JSON
import com.scprojekt.infrastructure.constants.Camel
import com.scprojekt.infrastructure.processor.JpaUrlProcessor
import com.scprojekt.infrastructure.processor.StringToUUidProcessor
import com.scprojekt.infrastructure.repository.UserJpaRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode
import java.util.*

@ApplicationScoped
class UserRestRoute : RouteBuilder() {

    @Inject
    private lateinit var baseUserRepository: UserJpaRepository

    @Inject
    private lateinit var stringToUuidProcessor: StringToUUidProcessor

    @Inject
    private lateinit var prepareJpaUrlProcessor: JpaUrlProcessor

    override fun configure() {
        restConfiguration()
            .bindingMode(RestBindingMode.json)
            .inlineRoutes(true)
            .dataFormatProperty("prettyPrint", "true")
            .apiContextPath("docs")
            .apiProperty("api.title", "Vicuna API")
            .apiProperty("api.version", "1.0")
            .apiProperty("cors", "true")
            .clientRequestValidation(true)
            .enableCORS(true)

        findByUUIDInDatabase()
        storeInDatabase()
        createUserReadonlyRoute()
        createUserStoreRoute()
    }

    private fun createUserStoreRoute(){
        rest("/api/store/user")
            .post("create").consumes(MEDIATYPE_JSON).type(User::class.java).to(DIRECT_CREATEUSER)
            .post("manage").consumes(MEDIATYPE_JSON).type(User::class.java).to(DRECT_MANAGEUSER)
            .post("delete").consumes(MEDIATYPE_JSON).type(User::class.java).to(DIRECT_DELETEUSER)

        from(DIRECT_CREATEUSER)
            .routeId("infra.rest.createuser")
            .log("create user")
            .transacted()
            .bean(baseUserRepository, "createEntity(\${body})")
            .description("creates a user")
            .log(LoggingLevel.INFO, "Entity created")
            .end()

        from(DRECT_MANAGEUSER)
            .routeId("infra.rest.manageuser")
            .log("manage user")
            .transacted()
            .bean(baseUserRepository, "updateEntity(\${body})")
            .log(LoggingLevel.INFO, "Entity updated")
            .end()

        from(DIRECT_DELETEUSER)
            .routeId("infra.rest.removeuser")
            .log("deleting user \${body}")
            .transacted()
            .bean(baseUserRepository, "removeEntity(\${body})")
            .log(LoggingLevel.INFO, "Entity deleted")
            .end()
    }

    private fun createUserReadonlyRoute() {
        rest("/api/read/user")
            .get("uuid/{uuid}").consumes(MEDIATYPE_JSON).type(UUID::class.java).to("direct:byUUID")
            .get("bytype/{type}").consumes(MEDIATYPE_JSON).type(UserType::class.java).to("direct:byType")
            .get("byname/{name}").consumes(MEDIATYPE_JSON).type(String::class.java).to("direct:byName")
            .get("bydescr/{description}").consumes(MEDIATYPE_JSON).type(String::class.java).to("direct:byDescription")

        from("direct:byUUID")
            .routeId("infra.rest.byuuid")
            .log(LoggingLevel.INFO,"user by uuid")
            .process(stringToUuidProcessor)
            .bean(baseUserRepository, "findByUUID(\${header.uuid})")
            //.marshal().json()
            .log(LoggingLevel.INFO, "user by \${header.uuid} found")
            .end()

        from("direct:byType")
            .routeId("infra.rest.bytype")
            .log(LoggingLevel.INFO,"user by id")
            .bean(baseUserRepository, "findByType(\${header.type})")
            .log(LoggingLevel.INFO, "users by type found")
            .end()

        from("direct:byName")
            .routeId("infra.rest.byname")
            .log(LoggingLevel.INFO,"user by name")
            .bean(baseUserRepository, "findByName(\${header.name})")
            .log(LoggingLevel.INFO, "user by name found")
            .end()

        from("direct:byDescription")
            .routeId("infra.rest.bydescription")
            .log(LoggingLevel.INFO,"user by uuid")
            .bean(UserJpaRepository::class.java, "findByDescription(\${header.description})")
            .log(LoggingLevel.INFO, "users by description found")
            .end()
    }

    private fun storeInDatabase(){
        from(DIRECT_SAVEINDATABASE)
            .transacted()
            .process(prepareJpaUrlProcessor)
            .choice()
            .`when`(exchangeProperty(Camel.JPA_URL).isNotNull())
            .to("jpa://" + exchangeProperty(Camel.JPA_URL).toString())
            .otherwise()
            .log(LoggingLevel.INFO, Camel.NODATABASE_URL)
            .end()
    }

    private fun findByUUIDInDatabase(){
        from(DIRECT_FINDBYUUID)
            .transacted()
            //.process(stringToUuidProcessor)
            .bean(UserJpaRepository::class.java, "findByUUID(\${header.uuid})")
            .marshal().json()
            .log(LoggingLevel.INFO, "user by \${header.uuid} found")
            .end()
    }
}