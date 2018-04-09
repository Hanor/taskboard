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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import objective.taskboard.auth.LoggedUserDetails;

@Component
public class SpringContextBridge implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    public static <T> T getBean(Class<T> klass) {
        if (applicationContext == null)
            return null;
        return applicationContext.getBean(klass);
    }
    
    @Bean
    public SimpMessagingTemplate createMTemplate() {
        MessageChannel messageChannel = new MessageChannel() {
            @Override
            public boolean send(Message<?> message, long timeout) {
                return true;
            }
            
            @Override
            public boolean send(Message<?> message) {
                return true;
            }
        };
        return new SimpMessagingTemplate(messageChannel);
    }
    
    @Bean
    public LoggedUserDetails createFakeUser() {
        return new LoggedUserDetails() {
            @Override
            public List<Role> getUserRoles() {
                return Collections.emptyList();
            }
        };
    }
}
