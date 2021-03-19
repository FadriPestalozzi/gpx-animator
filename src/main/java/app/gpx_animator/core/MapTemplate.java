/*
 *  Copyright Contributors to the GPX Animator project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package app.gpx_animator.core;

@SuppressWarnings("PMD.BeanMembersShouldSerialize") // This class is not serializable
public final class MapTemplate {

    private final String id;

    private final String name;

    private final String url;

    private final String attributionText;


    public MapTemplate(final String id, final String name, final String url, final String attributionText) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.attributionText = attributionText;
    }


    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }


    @Override
    public String toString() {
        return name;
    }


    public String getAttributionText() {
        return attributionText;
    }

}
