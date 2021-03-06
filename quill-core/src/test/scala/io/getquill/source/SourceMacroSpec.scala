package io.getquill.source

import io.getquill.quotation.Quoted
import io.getquill._
import io.getquill.source.mirror.mirrorSource
import io.getquill.source.mirror.Row

class SourceMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Action[TestEntity]]
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Action[TestEntity]] = quote {
          qr1.delete
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a).delete
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == ?).delete"
        r.bindList mustEqual List(Row("a"))
      }
      "infix" in {
        val q = quote {
          (a: String) => infix"t = $a".as[Action[TestEntity]]
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $?""""
        r.bindList mustEqual List(Row("a"))
      }
      "dynamic" in {
        val q: Quoted[String => Action[TestEntity]] = quote {
          (a: String) => infix"t = $a".as[Action[TestEntity]]
        }
        val r = mirrorSource.run(q)(List("a"))
        r.ast.toString mustEqual """infix"t = $?""""
        r.bindList mustEqual List(Row("a"))
      }
    }
  }

  "runs queries" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Query[TestEntity]].map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "dynamic" in {
        val q: Quoted[Query[String]] = quote {
          qr1.map(t => t.s)
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a)
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == ?).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }
      "infix" in {
        val q = quote {
          (a: String) => infix"SELECT $a".as[Query[String]]
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual """infix"SELECT $?".map(x => x)"""
        r.binds mustEqual Row("a")
      }
      "dynamic" in {
        val q: Quoted[String => Query[TestEntity]] = quote {
          (a: String) => qr1.filter(t => t.s == a)
        }
        val r = mirrorSource.run(q)("a")
        r.ast.toString mustEqual "query[TestEntity].filter(t => t.s == ?).map(t => (t.s, t.i, t.l, t.o))"
        r.binds mustEqual Row("a")
      }
    }
    "aggregated" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      mirrorSource.run(q).ast mustEqual q.ast
    }
  }
}
