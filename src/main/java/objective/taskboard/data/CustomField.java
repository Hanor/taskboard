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
package objective.taskboard.data;

import java.io.Serializable;

public class CustomField implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldId;
    private Serializable value;
    private Long optionId;
    
    public CustomField(){}

    public CustomField(String fieldId, Serializable value) {
        this(fieldId, value, null);
    }

    public CustomField(String fieldId, Serializable value, Long optionId) {
        this.fieldId = fieldId;
        this.value = value;
        this.optionId = optionId;
    }
    
    public String getFieldId() {
        return fieldId;
    }

    public Object getValue() {
        return this.value;
    }

    public Long getOptionId() {
        return this.optionId;
    }

}
