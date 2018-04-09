/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */
package objective.taskboard.config;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.annotation.RequestScope;

import com.google.common.collect.Lists;

import objective.taskboard.auth.CredentialsHolder;
import objective.taskboard.auth.LoggedUserDetails;
import objective.taskboard.jira.JiraService;


public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private RESTAuthenticationFailureHandler authenticationFailureHandler;
    @Autowired
    private JiraService jiraService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(HttpMethod.POST, "/webhook/**").permitAll()
                .anyRequest().authenticated().and().formLogin();

        // allow using iframe to download files from same origin
        http.headers().frameOptions().sameOrigin();

        http.authorizeRequests().antMatchers("/api/*").authenticated().and().httpBasic();
        
        http.formLogin().loginPage("/login").permitAll();
        http.formLogin().failureHandler(authenticationFailureHandler);
        http.logout().permitAll();
        http.csrf().disable();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.eraseCredentials(false).authenticationProvider(new AuthenticationProvider() {

            @Override
            public boolean supports(Class<?> authentication) {
                return authentication.equals(UsernamePasswordAuthenticationToken.class);
            }

            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String name = authentication.getName();
                String password = authentication.getCredentials().toString();

                try {
                    jiraService.authenticate(name, password);
                }catch(Exception e) {//NOSONAR
                    throw new BadCredentialsException(e.getMessage());
                }
                return new UsernamePasswordAuthenticationToken(name, password, Lists.newArrayList());
            }

        });
    }

    @Bean
    @RequestScope
    public LoggedUserDetails getLoggedUserDetails() {
        List<LoggedUserDetails.Role> roles = jiraService.getUserRoles(CredentialsHolder.username()).stream()
            .map(r -> new LoggedUserDetails.Role(r.id, r.name, r.projectKey))
            .collect(toList());

        return new LoggedUserDetails() {
            @Override
            public List<Role> getUserRoles() {
                return roles;
            }
        };
    }

}