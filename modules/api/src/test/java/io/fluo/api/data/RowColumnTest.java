/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fluo.api.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link RowColumn}
 */
public class RowColumnTest {

  @Test
  public void testBasic() {
   
    RowColumn rc = new RowColumn();
    Assert.assertEquals(RowColumn.EMPTY, rc);
    Assert.assertEquals(Bytes.EMPTY, rc.getRow());
    Assert.assertEquals(Column.EMPTY, rc.getColumn());
    Assert.assertEquals("   ", rc.toString());
    Assert.assertNotEquals(RowColumn.EMPTY, Column.EMPTY);
    
    rc = new RowColumn(Bytes.wrap("r1"));
    Assert.assertEquals(Bytes.wrap("r1"), rc.getRow());
    Assert.assertEquals(Column.EMPTY, rc.getColumn());
    Assert.assertEquals(new RowColumn("r1"), rc);
    Assert.assertEquals("r1   ", rc.toString());
    
    rc = new RowColumn("r2", new Column("cf2"));
    Assert.assertEquals(Bytes.wrap("r2"), rc.getRow());
    Assert.assertEquals(new Column("cf2"), rc.getColumn());
    Assert.assertEquals(new RowColumn(Bytes.wrap("r2"), new Column("cf2")), rc);
    Assert.assertEquals("r2 cf2  ", rc.toString());
    Assert.assertEquals(132689, rc.hashCode());
  }
  
  @Test
  public void testFollowing() {
    byte[] fdata = new String("data1").getBytes();
    fdata[4] = (byte) 0x00;
    Bytes fb = Bytes.wrap(fdata);
    
    Assert.assertEquals(RowColumn.EMPTY, new RowColumn().following());
    Assert.assertEquals(new RowColumn(fb), new RowColumn("data").following());
    Assert.assertEquals(new RowColumn("row", new Column(fb)), new RowColumn("row", new Column("data")).following());
    Assert.assertEquals(new RowColumn("row", new Column(Bytes.wrap("cf"), fb)), new RowColumn("row", new Column("cf", "data")).following());
    Assert.assertEquals(new RowColumn("row", new Column(Bytes.wrap("cf"), Bytes.wrap("cq"), fb)), new RowColumn("row", new Column("cf", "cq", "data")).following());
  }
}