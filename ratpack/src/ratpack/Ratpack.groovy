import com.alvarosanchez.teams.ratpack.JwtAuthentication
import com.alvarosanchez.teams.ratpack.Team
import com.alvarosanchez.teams.ratpack.TeamsService
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ratpack.groovy.sql.SqlModule
import ratpack.h2.H2Module
import ratpack.handling.RequestLogger

import static ratpack.groovy.Groovy.groovyHandler
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.fromJson

ratpack {

  serverConfig {
    props(getClass().getResource("/teams.properties"))
  }

  bindings {
    module SqlModule
    module H2Module

    bind TeamsService
  }

  handlers { TeamsService teamsService ->

    post('login', JwtAuthentication.login())

    prefix('teams') {

      all JwtAuthentication.authenticate()

      path(':teamId') {

        byMethod {

          get {
            teamsService.findById(allPathTokens.asLong('teamId')).subscribe { Team team ->
              response.send JsonOutput.toJson(team)
            }
          }

          put {
            request.body.then{ body ->
              String name = new JsonSlurper().parseText(body.text).name
              Long id = allPathTokens.asLong('teamId')
              Team team = new Team(id: id, name: name)
              teamsService.update(team).subscribe {
                response.send JsonOutput.toJson(success: it > 0)
              }
            }
          }

          delete {
            teamsService.delete(allPathTokens.asLong('teamId')).subscribe {
              response.send JsonOutput.toJson(success: it > 0)
            }
          }

        }
      }

      path {
        byMethod {

          get {
            teamsService.list().toList().subscribe { List<Team> teams ->
              response.send JsonOutput.toJson(teams)
            }
          }

          post {
            request.body.then{ body ->
              String name = new JsonSlurper().parseText(body.text).name
              teamsService.create(name).subscribe {
                response.send JsonOutput.toJson(success: it.size() > 0)
              }
            }
          }

        }

      }

    }

  }
}
